package com.sparta.logistics.hub.hubstock.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.kafka.event.*;
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
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubStockService {

    private final HubStockRepository hubStockRepository;
    private final HubRepository hubRepository;
    private final HubStockLockHelper hubStockLockHelper;
    private final HubStockLogRepository hubStockLogRepository;
    private final HubStockEventPublisher hubStockEventPublisher;

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

        return executeWithLock(
                () -> hubStockLockHelper.adjustWithOptimisticLock(hubId, stockId, request),
                () -> hubStockLockHelper.adjustWithPessimisticLock(hubId, stockId, request)
        );
    }

    @Transactional
    public void restoreStock(RestoreStockCommand command) {

        List<RestoreStockItemPayload> succeededItems = new ArrayList<>();

        for (RestoreStockItemPayload item : command.getOrderItems()) {

            try {
                executeWithLock(
                        () -> { hubStockLockHelper.restoreWithOptimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId(), null
                        ); return null; },
                        () -> { hubStockLockHelper.restoreWithPessimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId(), null
                        ); return null; }
                );

                succeededItems.add(item);

            } catch (BusinessException e) {

                // 재고 없음 → 복구 실패 이벤트 발행 후 컨슈머 재시도 제외
                log.error("[HubStock] 재고 복구 실패. orderId: {}, productId: {}, reason: {}",
                        command.getOrderId(), item.getProductId(), e.getMessage());

                compensateRestoredItems(succeededItems);

                registerStockRestorationFailedEvent(
                        command.getOrderId(), e.getMessage()
                );

                throw new KafkaSkipException("재고 복구 실패 - orderId: " + command.getOrderId());
            }
        }

        // DB 커밋 성공 후 ack 발행 (커밋 전 발행 시 정합성 문제 방지)
        registerStockRestoredAckEvent(command.getOrderId());
    }

    @Transactional
    public void restoreOnDeliveryFailed(DeliveryCreationFailedEvent event) {

        List<RestoreStockItemPayload> succeededItems = new ArrayList<>();

        for (RestoreStockItemPayload item : event.getItemsToRestore()) {

            try {
                executeWithLock(
                        () -> { hubStockLockHelper.restoreWithOptimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId(), event.getDeliveryId()
                        ); return null; },
                        () -> { hubStockLockHelper.restoreWithPessimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId(), event.getDeliveryId()
                        ); return null; }
                );

                succeededItems.add(item);

            } catch (BusinessException e) {

                // 재고 없음 → 복구 실패 이벤트 발행 후 컨슈머 재시도 제외
                log.error("[HubStock] 배송 실패 재고 복구 실패. orderId: {}, productId: {}, reason: {}",
                        event.getOrderId(), item.getProductId(), e.getMessage());

                compensateRestoredItems(succeededItems);

                throw new KafkaSkipException("재고 복구 실패 - orderId: " + event.getOrderId());
            }
        }
    }


    @Transactional
    public void reserveStock(OrderCreatedEvent event) {

        try {
            hubRepository.findByIdAndDeletedAtIsNull(event.getSourceHubId())
                    .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

            hubRepository.findByIdAndDeletedAtIsNull(event.getDestinationHubId())
                    .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        } catch (BusinessException e) {
            log.warn("[HubStock] 허브 조회 실패. orderId: {}, sourceHubId: {}, destinationHubId: {}",
                    event.getOrderId(), event.getSourceHubId(), event.getDestinationHubId());
            // 허브 조회 실패는 특정 상품과 무관하므로 productId null 반환
            registerStockReservationFailedEvent(
                    event.getOrderId(),
                    null,
                    e.getMessage()
            );
            throw new KafkaSkipException("허브 조회 실패 - orderId: " + event.getOrderId());
        }


        // stock.reserved 발행 시 필요한 리스트
        List<StockReservedItemPayload> reservedItems = new ArrayList<>();

        List<OrderItemPayload> succeededItems = new ArrayList<>();

        for (OrderItemPayload item : event.getOrderItems()) {

            // 낙관적 락으로 재고 예약 시도, 충돌 시 비관적 락으로 폴백
            try {
                executeWithLock(
                        () -> {
                            hubStockLockHelper.reserveWithOptimisticLock(
                                    item.getHubId(), item.getProductId(),
                                    item.getQuantity(), item.getOrderItemId()
                            );
                            return null;
                        },
                        () -> {
                            hubStockLockHelper.reserveWithPessimisticLock(
                                    item.getHubId(), item.getProductId(),
                                    item.getQuantity(), item.getOrderItemId()
                            );
                            return null;
                        }
                );

                succeededItems.add(item);

            } catch (BusinessException e) {

                // 재고 없음 or 재고 부족 → 실패 이벤트 발행 후 컨슈머 재시도 제외
                log.warn("[HubStock] 재고 예약 실패. orderId: {}, productId: {}, reason: {}",
                        event.getOrderId(), item.getProductId(), e.getMessage());

                compensateReservedItems(succeededItems);

                registerStockReservationFailedEvent(
                        event.getOrderId(),
                        item.getProductId(),
                        e.getMessage()
                );

                throw new KafkaSkipException("재고 예약 실패 - orderId: " + event.getOrderId());
            }

            reservedItems.add(StockReservedItemPayload.builder()
                    .orderItemId(item.getOrderItemId())
                    .productId(item.getProductId())
                    .reservedQuantity(item.getQuantity())
                    .sourceHubId(item.getHubId())
                    .build()
            );
        }

        // DB 커밋 성공 후 stock.reserved 발행 (커밋 전 발행 시 정합성 문제 방지)

        StockReservedEvent reservedEvent = StockReservedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(event.getOrderId())
                .receiverId(event.getReceiverId())
                .sourceHubId(event.getSourceHubId())
                .destinationHubId(event.getDestinationHubId())
                .deliveryAddress(event.getDeliveryAddress())
                .orderItems(reservedItems)
                .build();
        registerStockReservedEvent(reservedEvent);
    }

    @Transactional
    public void deductReservedStock(DeliveryStartedEvent event) {

        for (DeliveryOrderItemPayload item : event.getOrderItems()) {

            try {
                executeWithLock(
                        () -> { hubStockLockHelper.deductWithOptimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId(), event.getDeliveryId()
                        ); return null; },
                        () -> { hubStockLockHelper.deductWithPessimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId(), event.getDeliveryId()
                        ); return null; }
                );
            } catch (BusinessException e) {

                // 재고 없음 → 컨슈머 재시도 제외
                log.error("[HubStock] 예약 재고 차감 실패. orderId: {}, productId: {}, reason: {}",
                        event.getOrderId(), item.getProductId(), e.getMessage());

                throw new KafkaSkipException("예약 재고 차감 실패 - orderId: " + event.getOrderId());
            }
        }
    }

    // 재고 복구 실패 시 발행
    private void registerStockRestorationFailedEvent(UUID orderId, String reason) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    // KafkaSkipException 발생 시 트랜잭션 롤백 → afterCommit() 미실행
                    // 롤백 후에도 실패 이벤트 발행이 필요하므로 afterCompletion 사용
                    @Override
                    public void afterCompletion(int status) {
                        hubStockEventPublisher.publishStockRestorationFailed(orderId, reason);
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

    private void registerStockRestoredAckEvent(UUID orderId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        hubStockEventPublisher.publishStockRestoredAck(orderId);
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

    private void registerStockReservationFailedEvent(UUID orderId, UUID productId, String reason) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        hubStockEventPublisher.publishStockReservationFailed(
                                orderId, productId, reason
                        );
                    }
                }
        );
    }

    // restoreStock, restoreOnDeliveryFailed 보상 — 복구한 재고를 다시 예약 상태로 되돌리기
    private void compensateRestoredItems(List<RestoreStockItemPayload> items) {
        for (RestoreStockItemPayload item : items) {
            try {
                executeWithLock(
                        () -> { hubStockLockHelper.compensateRestoreWithOptimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId()
                        ); return null; },
                        () -> { hubStockLockHelper.compensateRestoreWithPessimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId()
                        ); return null; }
                );
            } catch (Exception e) {
                log.error("[HubStock] 보상 트랜잭션 실패 - 수동 처리 필요. hubId: {}, productId: {}, quantity: {}",
                        item.getHubId(), item.getProductId(), item.getQuantity(), e);
            }
        }
    }

    // reserveStock 보상 — 예약한 재고를 다시 복구
    private void compensateReservedItems(List<OrderItemPayload> items) {
        for (OrderItemPayload item : items) {
            try {
                executeWithLock(
                        () -> { hubStockLockHelper.compensateReserveWithOptimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId()
                        ); return null; },
                        () -> { hubStockLockHelper.compensateReserveWithPessimisticLock(
                                item.getHubId(), item.getProductId(),
                                item.getQuantity(), item.getOrderItemId()
                        ); return null; }
                );
            } catch (Exception e) {
                log.error("[HubStock] 보상 트랜잭션 실패 - 수동 처리 필요. hubId: {}, productId: {}, quantity: {}",
                        item.getHubId(), item.getProductId(), item.getQuantity(), e);
            }
        }
    }

    private <T> T executeWithLock(Supplier<T> optimistic, Supplier<T> pessimistic) {

    for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
      try {
        return optimistic.get();
      } catch (OptimisticLockingFailureException e) {
        if (attempt == MAX_RETRY - 1) {
          return pessimistic.get();
        }
      }
    }
    throw new BusinessException(HubStockErrorCode.HUB_STOCK_ADJUST_FAILED);
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
