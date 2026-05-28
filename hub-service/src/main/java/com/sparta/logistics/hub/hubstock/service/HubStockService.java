package com.sparta.logistics.hub.hubstock.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.kafka.event.*;
import com.sparta.logistics.hub.client.CompanyClient;
import com.sparta.logistics.hub.exception.HubErrorCode;
import com.sparta.logistics.hub.exception.HubStockErrorCode;
import com.sparta.logistics.hub.hub.entity.Hub;
import com.sparta.logistics.hub.hub.repository.HubRepository;
import com.sparta.logistics.hub.hubstock.dto.request.AdjustHubStockRequest;
import com.sparta.logistics.hub.hubstock.dto.request.CreateHubStockRequest;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockAdjustResponse;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockCreateResponse;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockListResponse;
import com.sparta.logistics.hub.hubstock.entity.HubStock;
import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import com.sparta.logistics.hub.kafka.exception.KafkaSkipException;
import com.sparta.logistics.hub.kafka.publisher.HubStockEventPublisher;
import com.sparta.logistics.hub.hubstock.service.helper.HubStockLockHelper;
import com.sparta.logistics.hub.hubstock.repository.HubStockRepository;
import com.sparta.logistics.hub.hubstocklog.entity.HubStockLog;
import com.sparta.logistics.hub.hubstocklog.repository.HubStockLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubStockService {

    private final HubStockRepository hubStockRepository;
    private final HubRepository hubRepository;
    private final HubStockLockHelper hubStockLockHelper;
    private final HubStockLogRepository hubStockLogRepository;
    private final HubStockEventPublisher hubStockEventPublisher;
    private final CompanyClient companyClient;

    private static final int MAX_RETRY = 3;

    // todo: product도 존재 여부를 체크 고민
    @Transactional
    public HubStockCreateResponse createHubStock(UUID hubId, CreateHubStockRequest request, Role role, UUID userHubId) {

        // 허브 재고 권한 검증
        checkHubStockPermission(role, userHubId, hubId);

        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        // 중복 체크
        if (hubStockRepository.existsByHubAndProductIdAndDeletedAtIsNull(hub, request.getProductId())) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_ALREADY_EXISTS);
        }

        // 레이스 컨디션 처리
        try {
            HubStock hubStock = HubStock.create(hub, request.getProductId(), request.getInitialQuantity());
            HubStock savedStock = hubStockRepository.save(hubStock);
            hubStockRepository.flush();

            // 재고 변경 이력 생성(입고 시에는 주문과 배송이 존재하지 않음)
            hubStockLogRepository.save(HubStockLog.create(
                    savedStock,
                    null,
                    null,
                    request.getInitialQuantity(),
                    0,
                    request.getInitialQuantity(),
                    HubStockChangeType.INBOUND
            ));

            registerHubStockUpdatedEvent(savedStock);

            return HubStockCreateResponse.from(savedStock);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public Page<HubStockListResponse> getHubStockList(UUID hubId, UUID productId, Pageable pageable, Role role, UUID userHubId) {

        // 허브 재고 권한 검증
        checkHubStockPermission(role, userHubId, hubId);

        return hubStockRepository.findAllByCondition(hubId, productId, pageable)
                .map(HubStockListResponse::from);
    }

    @Transactional
    public HubStockAdjustResponse adjustHubStock(UUID hubId, UUID stockId, AdjustHubStockRequest request, Role role, UUID userHubId) {

        // 허브 재고 권한 검증
        checkHubStockPermission(role, userHubId, hubId);

        // changeType 유효성 검증
        if (request.getChangeType() != HubStockChangeType.INBOUND &&
                request.getChangeType() != HubStockChangeType.MANUAL_ADJUST) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_INVALID_CHANGE_TYPE);
        }

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return hubStockLockHelper.adjustWithOptimisticLock(hubId, stockId, request);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRY - 1) {
                    return hubStockLockHelper.adjustWithPessimisticLock(hubId, stockId, request);
                }
            }
        }

        throw new BusinessException(HubStockErrorCode.HUB_STOCK_ADJUST_FAILED);
    }

    @Transactional
    public void restoreStock(RestoreStockCommand command) {

        for (RestoreStockItemPayload item : command.getOrderItems()) {

            HubStock hubStock = hubStockRepository
                    .findByHubIdAndProductId(item.getHubId(), item.getProductId())
                    .orElseGet(() -> {
                        registerStockRestorationFailedEvent(
                                command.getEventId(),
                                command.getOrderId(),
                                "허브 재고 없음"
                        );
                        throw new KafkaSkipException("허브 재고 없음 - orderId: " + command.getOrderId());
                    });

            int beforeQuantity = hubStock.getAvailable();
            hubStock.restore(item.getQuantity());
            int afterQuantity = hubStock.getAvailable();

            // 재고 변경 이력 기록(주문 취소 시에는 주문만 존재하고 배송은 존재하지 않음)
            hubStockLogRepository.save(HubStockLog.create(
                    hubStock,
                    item.getOrderItemId(),
                    null,
                    item.getQuantity(),
                    beforeQuantity,
                    afterQuantity,
                    HubStockChangeType.CANCEL_RESTORE
            ));

            registerHubStockUpdatedEvent(hubStock);
        }

        // DB 커밋 성공 후 ack 발행 (커밋 전 발행 시 정합성 문제 방지)
        registerStockRestoredAckEvent(command.getEventId(), command.getOrderId());
    }

    @Transactional
    public void restoreOnDeliveryFailed(DeliveryCreationFailedEvent event) {

        for (RestoreStockItemPayload item : event.getItemsToRestore()) {

            HubStock hubStock = hubStockRepository
                    .findByHubIdAndProductId(item.getHubId(), item.getProductId())
                    .orElseGet(() -> {
                        log.error("[HubStock] 재고 복구 실패 - 허브 재고 없음. orderId: {}, productId: {}",
                                event.getOrderId(), item.getProductId());
                        throw new KafkaSkipException("허브 재고 없음 - orderId: " + event.getOrderId());
                    });

            int beforeQuantity = hubStock.getAvailable();
            hubStock.restore(item.getQuantity());
            int afterQuantity = hubStock.getAvailable();

            // 재고 변경 이력 추가
            hubStockLogRepository.save(HubStockLog.create(
                    hubStock,
                    item.getOrderItemId(),
                    event.getDeliveryId(),   // 생성 실패 시 null 가능
                    item.getQuantity(),
                    beforeQuantity,
                    afterQuantity,
                    HubStockChangeType.CANCEL_RESTORE
            ));

            registerHubStockUpdatedEvent(hubStock);
        }
    }

    @Transactional
    public void reserveStock(OrderCreatedEvent event) {

        // stock.reserved 발행 시 필요
        UUID destinationHubId = companyClient
                .getCompany(event.getReceiverCompanyId()).getHubId();

        // stock.reserved 발행 시 필요한 리스트
        List<StockReservedItemPayload> reservedItems = new ArrayList<>();

        for (OrderItemPayload item : event.getOrderItems()) {

            HubStock hubStock = hubStockRepository
                    .findByHubIdAndProductId(item.getHubId(), item.getProductId())
                    .orElseGet(() -> {
                        registerStockReservationFailedEvent(
                                event.getEventId(),
                                event.getOrderId(),
                                item.getProductId(),
                                "허브 재고 없음"
                        );
                        throw new KafkaSkipException("허브 재고 없음 - orderId: " + event.getOrderId());
                    });

            // 재고 부족 시 실패 이벤트 발행하고 종료
            if (hubStock.getAvailable() < item.getQuantity()) {
                registerStockReservationFailedEvent(
                        event.getEventId(),
                        event.getOrderId(),
                        item.getProductId(),
                        "재고 부족");
                throw new KafkaSkipException("재고 부족 - orderId: " + event.getOrderId());
            }

            int beforeQuantity = hubStock.getAvailable();
            hubStock.reserve(item.getQuantity());
            int afterQuantity = hubStock.getAvailable();

            // 재고 변경 이력 기록
            hubStockLogRepository.save(HubStockLog.create(
                    hubStock,
                    item.getOrderItemId(),
                    null,
                    item.getQuantity(),
                    beforeQuantity,
                    afterQuantity,
                    HubStockChangeType.ORDER_RESERVE
            ));

            // 아이템마다 sourceHubId 포함해서 리스트에 추가
            reservedItems.add(StockReservedItemPayload.builder()
                    .productId(item.getProductId())
                    .reservedQuantity(item.getQuantity())
                    .sourceHubId(item.getHubId())
                    .build()
            );

            registerHubStockUpdatedEvent(hubStock);
        }

        // DB 커밋 성공 후 stock.reserved 발행 (커밋 전 발행 시 정합성 문제 방지)
        StockReservedEvent reservedEvent = StockReservedEvent.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .destinationHubId(destinationHubId)
                .orderItems(reservedItems)
                .build();
        registerStockReservedEvent(reservedEvent);
    }

    @Transactional
    public void deductReservedStock(DeliveryStartedEvent event) {

        for (DeliveryOrderItemPayload item : event.getOrderItems()) {

            HubStock hubStock = hubStockRepository
                    .findByHubIdAndProductId(item.getHubId(), item.getProductId())
                    .orElseGet(() -> {
                        log.error("[HubStock] 예약 재고 차감 실패 - 허브 재고 없음. orderId: {}, productId: {}",
                                event.getOrderId(), item.getProductId());
                        throw new KafkaSkipException("허브 재고 없음 - orderId: " + event.getOrderId());
                    });

            int beforeReserved = hubStock.getReserved();
            hubStock.decreaseReserved(item.getQuantity());
            int afterReserved = hubStock.getReserved();

            hubStockLogRepository.save(HubStockLog.create(
                    hubStock,
                    item.getOrderItemId(),
                    event.getDeliveryId(),
                    -item.getQuantity(),
                    beforeReserved,
                    afterReserved,
                    HubStockChangeType.ORDER_DECREASE
            ));
        }
    }

    // 재고 복구 실패 시 발행
    private void registerStockRestorationFailedEvent(UUID eventId, UUID orderId, String reason) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    // KafkaSkipException 발생 시 트랜잭션 롤백 → afterCommit() 미실행
                    // 롤백 후에도 실패 이벤트 발행이 필요하므로 afterCompletion 사용
                    @Override
                    public void afterCompletion(int status) {
                        hubStockEventPublisher.publishStockRestorationFailed(eventId, orderId, reason);
                    }
                }
        );
    }

    // 재고 변경 후 Order Service 스냅샷 갱신을 위한 이벤트 등록 (커밋 후 발행)
    private void registerHubStockUpdatedEvent(HubStock hubStock) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        hubStockEventPublisher.publishHubStockUpdated(
                                hubStock.getProductId(),
                                hubStock.getHub().getId(),
                                hubStock.getAvailable(),
                                hubStock.getVersion()
                        );
                    }
                }
        );
    }

    private void registerStockRestoredAckEvent(UUID eventId, UUID orderId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        hubStockEventPublisher.publishStockRestoredAck(eventId, orderId);
                    }
                }
        );
    }

    private void registerStockReservedEvent(StockReservedEvent event) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        hubStockEventPublisher.publishStockReserved(event);
                    }
                }
        );
    }

    private void registerStockReservationFailedEvent(UUID eventId, UUID orderId, UUID productId, String reason) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        hubStockEventPublisher.publishStockReservationFailed(
                                eventId, orderId, productId, reason
                        );
                    }
                }
        );
    }
    
    private void checkHubStockPermission(Role role, UUID userHubId, UUID hubId) {

        if (role == Role.MASTER) return;

        if (role == Role.HUB_MANAGER) {
            if (userHubId == null || !userHubId.equals(hubId)) {
                throw new BusinessException(HubStockErrorCode.HUB_STOCK_FORBIDDEN);
            }
            return;
        }

        throw new BusinessException(HubStockErrorCode.HUB_STOCK_FORBIDDEN);
    }
}
