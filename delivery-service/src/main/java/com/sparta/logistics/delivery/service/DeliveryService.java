package com.sparta.logistics.delivery.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.delivery.client.response.HubRouteSegmentResponse;
import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.dto.DeliveryListResponse;
import com.sparta.logistics.delivery.dto.DeliverySearchCond;
import com.sparta.logistics.delivery.dto.DeliveryStatusChangeRequest;
import com.sparta.logistics.delivery.dto.DeliveryUpdateRequest;
import com.sparta.logistics.delivery.dto.event.StockReservedEventDto;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.DeliveryLogEntity;
import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import com.sparta.logistics.delivery.entity.DeliveryRouteEntity;
import com.sparta.logistics.delivery.entity.enums.DeliveryStatus;
import com.sparta.logistics.delivery.entity.enums.DeliveryEventType;
import com.sparta.logistics.delivery.entity.enums.RouteType;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import com.sparta.logistics.delivery.kafka.producer.DeliveryEventPublisher;
import com.sparta.logistics.delivery.repository.DeliveryLogRepository;
import com.sparta.logistics.delivery.repository.DeliveryOrderItemRepository;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import com.sparta.logistics.delivery.repository.DeliveryRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final DeliveryPermissionChecker permissionChecker;
    private final DeliveryEventPublisher eventPublisher;
    private final DeliveryAssignmentService assignmentService;

    // 배송 단건 조회
    @Transactional(readOnly = true)
    public DeliveryDetailResponse getDelivery(UUID deliveryId, UUID userId, Role role,
                                              UUID hubId, UUID companyId) {
        DeliveryEntity entity = findActiveOrThrow(deliveryId);
        permissionChecker.checkDeliveryReadPermission(entity, userId, role, hubId, companyId);
        return DeliveryDetailResponse.from(entity);
    }

    // 배송 목록 조회
    @Transactional(readOnly = true)
    public Page<DeliveryListResponse> getDeliveryList(UUID userId, Role role, UUID hubId,
                                                       Pageable pageable, DeliverySearchCond cond) {
        switch (role) {
            case HUB_MANAGER -> cond.setAuthorizedHubId(hubId);
            case DELIVERY_MANAGER -> cond.setAuthorizedManagerId(userId);
            case MASTER, COMPANY_MANAGER -> {}
        }
        Page<DeliveryEntity> deliveryPage = deliveryRepository.findAllByCondition(cond, pageable);
        return deliveryPage.map(d -> DeliveryListResponse.from(
                d,
                d.getSourceHubId() != null ? d.getSourceHubId().toString() : null,
                d.getDestinationHubId() != null ? d.getDestinationHubId().toString() : null,
                d.getCompanyDeliveryManagerId() != null ? d.getCompanyDeliveryManagerId().toString() : null
        ));
    }

    // 배송 수정
    @Transactional
    public DeliveryDetailResponse updateDelivery(UUID deliveryId, DeliveryUpdateRequest req,
                                                  UUID userId, Role role, UUID hubId) {
        DeliveryEntity entity = findActiveOrThrow(deliveryId);
        permissionChecker.checkDeliveryWritePermission(entity, userId, role, hubId);
        entity.update(req);
        return DeliveryDetailResponse.from(entity);
    }

    // 배송 상태 변경 (로그 동기 저장 — 같은 트랜잭션)
    @Transactional
    public DeliveryDetailResponse changeStatus(UUID deliveryId, DeliveryStatusChangeRequest req,
                                                UUID userId, Role role, UUID hubId) {
        DeliveryEntity entity = findActiveOrThrow(deliveryId);
        permissionChecker.checkDeliveryStatusChangePermission(entity, userId, role, hubId);

        entity.changeStatus(req.status());

        deliveryLogRepository.save(new DeliveryLogEntity(
                deliveryId, DeliveryEventType.STATUS_CHANGED, req.status(), null, null, userId
        ));
        return DeliveryDetailResponse.from(entity);
    }

    // 배송 삭제 (soft delete)
    @Transactional
    public void deleteDelivery(UUID deliveryId, UUID userId, Role role) {
        DeliveryEntity entity = findActiveOrThrow(deliveryId);
        permissionChecker.checkDeletePermission(role);
        entity.delete(userId);

        deliveryLogRepository.save(new DeliveryLogEntity(
                deliveryId, DeliveryEventType.CANCELLED, null, "배송 삭제", null, userId
        ));
    }

    // DeliveryEventHandler에서 Feign 호출 후 진입 — 트랜잭션 범위 최소화
    // DeliveryEntity + DeliveryRouteEntity[] 를 단일 트랜잭션으로 저장: 하나 실패 시 전체 롤백
    @Transactional
    public void createDelivery(StockReservedEventDto event, String slackId,
                               List<HubRouteSegmentResponse> routeSegments) {
        // 멱등성 보장: Kafka at-least-once 중복 소비 방어
        // orderId 단독 체크 시 같은 주문의 다른 허브 이벤트를 중복으로 차단하므로 orderId+sourceHubId 조합 사용
        if (deliveryRepository.existsByOrderIdAndSourceHubId(event.orderId(), event.sourceHubId())) {
            log.info("[createDelivery] 이미 처리된 주문+허브 조합 — orderId={}, sourceHubId={}", event.orderId(), event.sourceHubId());
            return;
        }

        DeliveryEntity entity = new DeliveryEntity(
                event.orderId(),
                event.receiverId(),
                event.sourceHubId(),
                event.destinationHubId(),
                event.deliveryAddress(),
                slackId
        );
        deliveryRepository.save(entity);

        // 생성 로그 — actorId는 시스템 생성이므로 null
        deliveryLogRepository.save(new DeliveryLogEntity(
                entity.getId(),
                DeliveryEventType.CREATED,
                entity.getStatus(),
                "orderId=" + event.orderId() + ", sourceHubId=" + event.sourceHubId(),
                null,
                null
        ));

        // hub-service 구간 정보로 DeliveryRoute 일괄 저장
        for (HubRouteSegmentResponse seg : routeSegments) {
            RouteType routeType = seg.lastMile() ? RouteType.HUB_TO_COMPANY : RouteType.HUB_TO_HUB;
            deliveryRouteRepository.save(new DeliveryRouteEntity(
                    entity,
                    seg.sequence(),
                    routeType,
                    seg.sourceHubId(),
                    seg.destinationHubId(),
                    seg.estimatedDistance(),
                    seg.estimatedDuration()
            ));
        }

        // delivery.started 발행을 위해 orderItems 저장
        for (var item : event.orderItems()) {
            deliveryOrderItemRepository.save(
                    new DeliveryOrderItemEntity(entity, item.orderItemId(), item.productId(), item.sourceHubId(), item.reservedQuantity())
            );
        }

        int totalEstimatedDuration = routeSegments.stream()
                .mapToInt(HubRouteSegmentResponse::estimatedDuration)
                .sum();

        // afterCommit에서 쓸 값 미리 캡처 (엔티티 detach 이후에도 안전하게 접근)
        UUID capturedDeliveryId = entity.getId();
        UUID capturedOrderId = event.orderId();
        UUID capturedSourceHubId = entity.getSourceHubId();
        UUID capturedDestinationHubId = entity.getDestinationHubId();
        UUID capturedManagerId = entity.getCompanyDeliveryManagerId();
        int capturedCount = event.totalDeliveryCount() != null ? event.totalDeliveryCount() : 0;
        String capturedAddress = entity.getDeliveryAddress();
        String capturedSlackId = entity.getReceiverSlackId();
        // 이벤트 record는 detach 무관하지만 스타일 통일
        String capturedSourceHubName = event.sourceHubName();
        String capturedDestinationHubName = event.destinationHubName();
        java.time.LocalDateTime capturedCreatedAt = entity.getCreatedAt();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 트랜잭션 커밋 후 배차: 새 트랜잭션에서 커밋된 DeliveryEntity를 읽을 수 있어 DELIVERY_NOT_FOUND 방지
                // 외부 트랜잭션 롤백과 무관하게 커밋되는 REQUIRES_NEW 문제도 동시 해결
                try {
                    assignmentService.assignManagersForSystem(capturedDeliveryId);
                } catch (Exception e) {
                    log.warn("[배송 생성] 담당자 배정 실패 — 스케줄러 재시도 예정, deliveryId={}", capturedDeliveryId, e);
                }
                try {
                    eventPublisher.publishCreated(
                            capturedDeliveryId, capturedOrderId,
                            capturedSourceHubId, capturedDestinationHubId,
                            capturedManagerId, capturedCount,
                            capturedAddress, totalEstimatedDuration,
                            capturedSlackId, capturedSourceHubName, capturedDestinationHubName,
                            capturedCreatedAt
                    );
                } catch (Exception e) {
                    log.error("[Kafka][수동처리 필요] delivery.created 발행 실패(afterCommit) — deliveryId={}",
                            capturedDeliveryId, e);
                }
            }
        });
    }

    // ai.deadline.calculated 이벤트 수신 시 호출 — deadline 저장 후 delivery.started 발행
    // TODO: deadline이 null이어도 delivery.started가 발행되게 할 것인지 결정
    @Transactional
    public void updateFinalDispatchDeadline(UUID deliveryId, LocalDateTime deadline) {
        DeliveryEntity entity = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        entity.updateFinalDispatchDeadline(deadline);

        List<DeliveryOrderItemEntity> items = deliveryOrderItemRepository.findByDelivery_Id(deliveryId);
        UUID orderId = entity.getOrderId();

        // 트랜잭션 커밋 성공 후 발행 — 롤백 시 delivery.started 미발행 보장
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    eventPublisher.publishStarted(deliveryId, orderId, items);
                    deliveryOrderItemRepository.deleteByDelivery_Id(deliveryId);
                } catch (Exception e) {
                    // offset 이미 커밋됨 — delivery.started 유실, 수동 처리 필요
                    log.error("[Kafka][수동처리 필요] delivery.started 발행 실패(afterCommit) — deliveryId={}",
                            deliveryId, e);
                }
            }
        });
    }

    // cancel.delivery.command 수신 시 호출 — 취소 가능 여부 확인 후 상태 전이
    @Transactional
    public boolean cancelDeliveryByCommand(UUID deliveryId) {
        DeliveryEntity entity = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND));

        DeliveryStatus status = entity.getStatus();

        if (status == DeliveryStatus.CANCELLED) {
            log.info("[Saga] 이미 취소된 배송 — deliveryId={}", deliveryId);
            return true;
        }

        if (status == DeliveryStatus.HUB_MOVING || status == DeliveryStatus.DESTINATION_HUB_ARRIVED
                || status == DeliveryStatus.OUT_FOR_DELIVERY || status == DeliveryStatus.COMPLETED) {
            log.warn("[Saga] 배송 취소 불가 — deliveryId={}, status={}", deliveryId, status);
            return false;
        }

        entity.changeStatus(DeliveryStatus.CANCELLED);
        deliveryLogRepository.save(new DeliveryLogEntity(
                deliveryId, DeliveryEventType.CANCELLED, DeliveryStatus.CANCELLED,
                "주문 취소 Saga 명령으로 배송 취소", null, null
        ));
        return true;
    }

    private DeliveryEntity findActiveOrThrow(UUID deliveryId) {
        DeliveryEntity entity = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        if (entity.isDeleted()) {
            throw new BusinessException(DeliveryErrorCode.DELIVERY_ALREADY_DELETED);
        }
        return entity;
    }
}
