package com.sparta.logistics.order.order.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.kafka.event.HubStockUpdatedEvent;
import com.sparta.logistics.order.kafka.producer.OrderEventPublisher;
import com.sparta.logistics.order.client.CompanyServiceClient;
import com.sparta.logistics.order.client.ProductServiceClient;
import com.sparta.logistics.order.client.response.CompanyResponse;
import com.sparta.logistics.order.client.response.ProductResponse;
import com.sparta.logistics.order.exception.OrderErrorCode;
import com.sparta.logistics.order.order.dto.response.OrderDetailResponse;
import com.sparta.logistics.order.order.dto.response.OrderSummaryResponse;
import com.sparta.logistics.order.order.entity.Order;
import com.sparta.logistics.order.order.enums.OrderStatus;
import com.sparta.logistics.order.order.lock.OrderLockManager;
import com.sparta.logistics.order.order.lock.OrderProcessStatus;
import com.sparta.logistics.order.order.entity.OrderDelivery;
import com.sparta.logistics.order.order.repository.OrderDeliveryRepository;
import com.sparta.logistics.order.order.repository.OrderRepository;
import com.sparta.logistics.order.order.saga.CancelOrderOrchestrator;
import com.sparta.logistics.order.orderitem.dto.request.OrderItemRequest;
import com.sparta.logistics.order.orderitem.entity.OrderItem;
import com.sparta.logistics.order.stock.entity.ProductStockSnapshot;
import com.sparta.logistics.order.stock.repository.ProductStockSnapshotRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final CompanyServiceClient companyServiceClient;
    private final ProductServiceClient productServiceClient;
    private final OrderEventPublisher orderEventPublisher;
    private final CancelOrderOrchestrator cancelOrderOrchestrator;
    private final ProductStockSnapshotRepository snapshotRepository;
    private final OrderLockManager orderLockManager;

    /** 주문 목록 조회 **/
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrders(
            UUID requesterCompanyId,
            UUID receiverCompanyId,
            OrderStatus status,
            LocalDateTime dueDateFrom,
            LocalDateTime dueDateTo,
            Role role,
            UUID userId,
            Pageable pageable
    ) {
        // MASTER, HUB_MANAGER는 전체 조회, 그 외는 본인 주문만 가능
        return orderRepository.search(
                isAdminRole(role) ? null : userId,
                requesterCompanyId,
                receiverCompanyId,
                status,
                dueDateFrom,
                dueDateTo,
                pageable
        ).map(OrderSummaryResponse::from);
    }

    /** 주문 단건 조회 **/
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(UUID orderId, UUID userId, Role role) {
        Order order = findOrder(orderId);
        checkPermission(order, userId, role);

        String requesterCompanyName = fetchCompanyName(order.getRequesterCompanyId());
        String receiverCompanyName = fetchCompanyName(order.getReceiverCompanyId());

        return OrderDetailResponse.from(order, requesterCompanyName, receiverCompanyName);
    }

    /** 주문 수정 **/
    @Transactional
    public OrderDetailResponse updateOrder(UUID orderId, LocalDateTime dueDate, String requestMemo, UUID userId, Role role, UUID userHubId) {
        // 수정은 HUB_MANAGER 또는 MASTER만 가능
        if (!isAdminRole(role)) {
            throw new BusinessException(OrderErrorCode.ORDER_UPDATE_PERMISSION_DENIED);
        }

        // 1. CANCELLING 상태 사전 차단
        orderLockManager.getStatusKey(orderId).ifPresent(s -> {
            if (s == OrderProcessStatus.CANCELLING) {
                throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CANCELLING);
            }
        });

        // 2. 분산 락 획득
        orderLockManager.acquireLock(orderId);
        try {
            Order order = findOrder(orderId);

            // HUB_MANAGER는 본인 담당 허브 소속 업체의 주문만 수정 가능
            if (role == Role.HUB_MANAGER) {
                checkHubPermission(order.getRequesterCompanyId(), userHubId);
            }

            // CANCELLED COMPLETED, IN_DELIVERY 상태는 수정 불가
            if (!order.isModifiable()) {
                throw new BusinessException(OrderErrorCode.ORDER_NOT_MODIFIABLE);
            }

            order.update(dueDate, requestMemo);
            return OrderDetailResponse.from(order);
        } finally {
            orderLockManager.releaseLock(orderId);
        }
    }

    /** 주문-업체 소속 확인 (내부 서비스 전용) **/
    @Transactional(readOnly = true)
    public boolean isOrderBelongsToCompany(UUID orderId, UUID companyId) {
        return orderRepository.existsByOrderIdAndCompanyId(orderId, companyId);
    }

    /** 업체 소속 주문 ID 목록 조회 (내부 서비스 전용) **/
    @Transactional(readOnly = true)
    public List<UUID> getOrderIdsByCompanyId(UUID companyId) {
        return orderRepository.findOrderIdsByCompanyId(companyId);
    }

    /** 주문 삭제 (Soft Delete) **/
    @Transactional
    public void deleteOrder(UUID orderId, UUID userId, Role role) {
        // MASTER만 삭제 가능
        if (role != Role.MASTER) {
            throw new BusinessException(OrderErrorCode.ORDER_DELETE_PERMISSION_DENIED);
        }

        Order order = findOrder(orderId);

        // CANCELLED 또는 COMPLETED 상태의 주문만 삭제 가능
        if (!order.isDeletable()) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_DELETABLE);
        }

        order.delete(userId);
        orderDeliveryRepository.deleteByOrderId(orderId);
    }

    // ===== 주문 생성 Choreography Saga =====

    /**
     * 주문 생성 (Choreography Saga Step 1-1 진입점)
     * 1. requesterCompanyId / receiverCompanyId 존재 여부 검증 (Company Service) ✅
     * 2. 스냅샷 기반 재고 사전 검증 (스냅샷이 있는 상품만, 없으면 통과) ✅
     * 3. 각 상품 정보(이름·단가) 조회 (Product Service) ✅
     * 4. Order 및 OrderItem 생성 (PENDING) ✅
     * 5. order.created 발행 → HubService 재고 예약 트리거 ✅
     * 이후 단계(배송 생성, AI 발송 시한 계산, Slack 알림)는 Kafka 체이닝으로 각 서비스에서 처리됨
     **/
    @Transactional
    public OrderDetailResponse createOrder(
            UUID requesterCompanyId,
            UUID receiverCompanyId,
            LocalDateTime dueDate,
            String requestMemo,
            List<OrderItemRequest> items,
            UUID userId
    ) {
        // 업체가 존재하는지 검증
        // 업체 조회 + 존재 검증 (hubId 추출 목적)
        CompanyResponse requesterCompany = fetchCompanyOrThrow(requesterCompanyId);
        CompanyResponse receiverCompany = fetchCompanyOrThrow(receiverCompanyId);
        UUID sourceHubId = requesterCompany.hubId();
        UUID destinationHubId = receiverCompany.hubId();
        String deliveryAddress = receiverCompany.address();

        // 동일 productId의 quantity 합산 (중복 OrderItem row 생성 방지)
        Map<UUID, Integer> mergedItems = items.stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequest::getProductId,
                        Collectors.summingInt(OrderItemRequest::getQuantity)
                ));

        // 스냅샷 기반 재고 사전 검증 (스냅샷이 없는 상품은 건너뜀)
        validateStockBySnapshot(mergedItems);

        Map<UUID, ProductResponse> productMap = fetchProducts(new ArrayList<>(mergedItems.keySet()));

        Order order = Order.create(requesterCompanyId, receiverCompanyId, userId, dueDate, requestMemo);

        mergedItems.forEach((productId, quantity) -> {
            ProductResponse product = productMap.get(productId);
            OrderItem orderItem = OrderItem.create(
                    order,
                    productId,
                    product.name(),
                    product.price(),
                    quantity,
                    product.hubId()
            );
            order.addOrderItem(orderItem);
        });

        order.calculateTotalAmount();
        orderRepository.save(order);


        log.info("[오더-> 배송 직전] 출발 허브{}, 도착허브{}", sourceHubId, destinationHubId);
        // Choreography Saga Step 1-1: order.created 이벤트 발행 → HubService 재고 예약 트리거
        orderEventPublisher.publishOrderCreated(order, sourceHubId, destinationHubId, deliveryAddress);

        return OrderDetailResponse.from(order);
    }

    /**
     * Choreography Saga Step 1-4: delivery.created 이벤트 수신 후 p_order_delivery에 deliveryId 누적 저장
     * 수신 건수가 totalDeliveryCount에 도달하면 주문 상태를 ACCEPTED로 전이함 (주문 1건 : 배송 N건 대응)
     * DeliveryCreatedConsumer에서 호출됨
     * <p>
     * 멱등성 보장:
     *   - PENDING 이외 상태는 이미 처리된 이벤트로 간주하고 무시함
     *   - 동일 (orderId, deliveryId) 쌍이 이미 저장되어 있으면 재저장하지 않음
     * <p>
     * 동시성 보장:
     *   - 동일 orderId에 대한 delivery.created 이벤트가 수 밀리초 내에 동시 도달하면
     *     countByOrderId 읽기와 order.accept() 전이 사이에 레이스 컨디션이 발생할 수 있음
     *   - 분산 락으로 직렬화하여 방지 (updateOrder / cancelOrder와 동일한 패턴)
     **/
    @Transactional
    public void acceptOrder(UUID orderId, UUID deliveryId, int totalDeliveryCount) {
        orderLockManager.acquireLock(orderId);
        try {
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order == null) {
                log.warn("[delivery.created] 주문을 찾을 수 없음 orderId={}", orderId);
                return;
            }

            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("[delivery.created] 이미 처리된 주문 orderId={} status={}", orderId, order.getStatus());
                return;
            }

            // 동일 deliveryId가 이미 저장되어 있으면 재저장하지 않음
            if (!orderDeliveryRepository.existsByOrderIdAndDeliveryId(orderId, deliveryId)) {
                orderDeliveryRepository.save(OrderDelivery.of(orderId, deliveryId));
            }
            order.linkDelivery(deliveryId);

            long receivedCount = orderDeliveryRepository.countByOrderId(orderId);
            if (receivedCount >= totalDeliveryCount) {
                order.accept();
                log.info("[delivery.created] 주문 ACCEPTED 전이 완료 orderId={} deliveryCount={}/{}",
                        orderId, receivedCount, totalDeliveryCount);
            } else {
                log.info("[delivery.created] 배송 부분 등록 orderId={} received={}/{}",
                        orderId, receivedCount, totalDeliveryCount);
            }
        } finally {
            orderLockManager.releaseLock(orderId);
        }
    }

    /**
     * Choreography Saga 보상 Step 2-1 / 2-2: 재고 예약 실패 또는 배송 생성 실패 시 주문을 즉시 CANCELLED 처리함
     * - Step 2-1: StockReservationFailedConsumer (stock.reservation.failed)
     * - Step 2-2: DeliveryCreationFailedConsumer (delivery.creation.failed)
     * <p>
     * 멱등성 보장: 이미 CANCELLED인 경우 재처리 시 no-op
     **/
    @Transactional
    public void cancelOrderByCompensation(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("[보상 취소] 주문을 찾을 수 없음 orderId={}", orderId);
            return;
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("[보상 취소] 멱등성 처리: 이미 취소된 주문 orderId={}", orderId);
            return;
        }

        order.cancel(null, reason);
        log.info("[보상 취소] 주문 CANCELLED orderId={} reason={}", orderId, reason);
    }

    // ===== 주문 취소 Orchestration Saga =====

    /**
     * 주문 취소 요청 (Orchestration Saga Step 3-1 진입점)
     * <p>
     * 권한 검사 + 주문 조회 완료 후 CancelOrderOrchestrator.start()에 위임
     * CANCELLING 전이 및 cancel.delivery.command 발행은 오케스트레이터가 담당
     **/
    @Transactional
    public OrderDetailResponse cancelOrder(UUID orderId, String cancelReason, UUID userId, Role role, UUID userHubId) {
        // 취소는 HUB_MANAGER 또는 MASTER만 가능
        if (!isAdminRole(role)) {
            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_PERMISSION_DENIED);
        }

        // 1. 상태 키 사전 차단 (fast-fail)
        orderLockManager.getStatusKey(orderId).ifPresent(s -> {
            if (s == OrderProcessStatus.PROCESSING) {
                throw new BusinessException(OrderErrorCode.ORDER_PROCESSING_IN_PROGRESS);
            }
            if (s == OrderProcessStatus.CANCELLING) {
                throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CANCELLING);
            }
        });

        // 2. 분산 락 획득
        orderLockManager.acquireLock(orderId);
        try {
            // 3. 락 획득 후 재확인
            orderLockManager.getStatusKey(orderId).ifPresent(s -> {
                if (s == OrderProcessStatus.PROCESSING) {
                    throw new BusinessException(OrderErrorCode.ORDER_PROCESSING_IN_PROGRESS);
                }
            });

            Order order = findOrder(orderId);

            // HUB_MANAGER는 본인 담당 허브 소속 업체의 주문만 취소 가능
            if (role == Role.HUB_MANAGER) {
                checkHubPermission(order.getRequesterCompanyId(), userHubId);
            }

            // CANCELLED, COMPLETED, IN_DELIVERY 상태는 취소 불가
            if (!order.isModifiable()) {
                throw new BusinessException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
            }

            // 4. 검증 통과 후 CANCELLING 세팅 (Saga 완료 또는 복구 시점에 해제됨)
            orderLockManager.setStatusKey(orderId, OrderProcessStatus.CANCELLING);

            // Orchestration Saga Step 3-1: CANCELLING 전이 + cancel.delivery.command 발행
            cancelOrderOrchestrator.start(order, userId, cancelReason);

            return OrderDetailResponse.from(order);
        } finally {
            // 5. 락 해제 (CANCELLING 상태 키는 Saga 완료/복구 시점에 해제)
            orderLockManager.releaseLock(orderId);
        }
    }

    /**
     * Orchestration Saga Step 3-3: delivery.cancelled.ack 수신 후 처리
     * DeliveryCancelledAckConsumer에서 호출됨 → CancelOrderOrchestrator.onDeliveryCancelled()에 위임
     **/
    public void handleDeliveryCancelled(UUID orderId) {
        cancelOrderOrchestrator.onDeliveryCancelled(orderId);
    }

    /**
     * Orchestration Saga Step 3-5: stock.restored.ack 수신 후 처리
     * StockRestoredAckConsumer에서 호출됨 → CancelOrderOrchestrator.onStockRestored()에 위임
     * Saga 완료 시점에 CANCELLING 상태 키를 해제함
     **/
    public void confirmOrderCancelled(UUID orderId) {
        cancelOrderOrchestrator.onStockRestored(orderId);
        orderLockManager.clearStatusKey(orderId);
    }

    /**
     * Orchestration Saga 보상 Step 4-1: delivery.cancellation.failed 수신 후 처리
     * DeliveryCancellationFailedConsumer에서 호출됨 → CancelOrderOrchestrator.onDeliveryCancellationFailed()에 위임
     * Saga 복구 시점에 CANCELLING 상태 키를 해제하여 이후 Consumer 이벤트가 정상 처리되도록 함
     **/
    public void handleDeliveryCancellationFailed(UUID orderId) {
        cancelOrderOrchestrator.onDeliveryCancellationFailed(orderId);
        orderLockManager.clearStatusKey(orderId);
    }

    /**
     * Orchestration Saga 보상 Step 4-2: stock.restoration.failed 수신 후 처리
     * StockRestorationFailedConsumer에서 호출됨 → CancelOrderOrchestrator.onStockRestorationFailed()에 위임
     **/
    public void handleStockRestorationFailed(UUID orderId) {
        cancelOrderOrchestrator.onStockRestorationFailed(orderId);
    }

    // ===== 재고 스냅샷 동기화 =====

    /**
     * 재고 스냅샷 동기화: hub.stock.updated 이벤트 수신 후 ProductStockSnapshot 갱신
     * HubStockUpdatedConsumer에서 호출됨
     * <p>
     * hubStockVersion 비교 → 저장된 버전보다 낮거나 같은 이벤트는 구버전으로 간주하고 무시함
     * 신규 상품: 스냅샷이 없는 경우 새로 생성함
     **/
    @Transactional
    public void syncSnapshot(HubStockUpdatedEvent event) {
        snapshotRepository.findByProductId(event.getProductId())
                .ifPresentOrElse(
                        s -> {
                            if (s.getHubStockVersion() >= event.getHubStockVersion()) {
                                log.warn("[hub.stock.updated] 구버전 이벤트 무시 productId={} storedVersion={} eventVersion={}",
                                        event.getProductId(), s.getHubStockVersion(), event.getHubStockVersion());
                                return;
                            }
                            s.update(event.getAvailable(), event.getHubStockVersion());
                            log.info("[hub.stock.updated] 스냅샷 갱신 productId={} available={} version={}",
                                    event.getProductId(), event.getAvailable(), event.getHubStockVersion());
                        },
                        () -> {
                            snapshotRepository.save(ProductStockSnapshot.create(
                                    event.getProductId(), event.getHubId(),
                                    event.getAvailable(), event.getHubStockVersion()
                            ));
                            log.info("[hub.stock.updated] 스냅샷 신규 생성 productId={} available={} version={}",
                                    event.getProductId(), event.getAvailable(), event.getHubStockVersion());
                        }
                );
    }

    // ===== Helper 메서드 =====

    /**
     * 스냅샷 기반 재고 사전 검증
     * 스냅샷이 있는 상품만 검사하며, 스냅샷이 없는 상품은 건너뜀
     * 재고 부족 시 Hub 서비스 호출 없이 즉시 예외를 던짐
     **/
    private void validateStockBySnapshot(Map<UUID, Integer> mergedItems) {
        mergedItems.forEach((productId, quantity) ->
                snapshotRepository.findByProductId(productId).ifPresent(snapshot -> {
                    if (snapshot.getAvailable() < quantity) {
                        log.warn("[재고 사전 검증] 재고 부족 productId={} available={} requested={}",
                                productId, snapshot.getAvailable(), quantity);
                        throw new BusinessException(OrderErrorCode.PRODUCT_STOCK_INSUFFICIENT);
                    }
                })
        );
    }

    private CompanyResponse fetchCompanyOrThrow(UUID companyId) {
        try {
            CompanyResponse company = companyServiceClient.getCompany(companyId).data();
            if (company == null) {
                throw new BusinessException(OrderErrorCode.COMPANY_NOT_FOUND);
            }
            return company;
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorCode.COMPANY_NOT_FOUND);
        } catch (FeignException e) {
            throw new BusinessException(OrderErrorCode.COMPANY_SERVICE_UNAVAILABLE);
        }
    }

    private Map<UUID, ProductResponse> fetchProducts(List<UUID> productIds) {
        try {
            // 배치 조회 방식으로 N+1 문제 해결
            List<ProductResponse> products = productServiceClient.getProducts(productIds).data();
            Map<UUID, ProductResponse> productMap = products.stream()
                    .collect(Collectors.toMap(ProductResponse::productId, p -> p));

            for (UUID productId: productIds) {
                ProductResponse product = productMap.get(productId);
                if (product == null) {
                    throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
                }
                // AVAILABLE 상태가 아닌 상품은 주문 불가
                if (!ProductResponse.STATUS_AVAILABLE.equals(product.status())) {
                    throw new BusinessException(OrderErrorCode.PRODUCT_NOT_AVAILABLE);
                }
            }
            return productMap;
        } catch (FeignException e) {
            throw new BusinessException(OrderErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
        }
    }

    private String fetchCompanyName(UUID companyId) {
        try {
            CompanyResponse company = companyServiceClient.getCompany(companyId).data();
            return company != null ? company.name() : null;
        } catch (FeignException e) {
            log.warn("[FeignClient] 업체 이름 조회 실패 companyId={} status={}", companyId, e.status());
            return null;
        }
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    // MASTER, HUB_MANAGER 이외의 사용자가 본인 것이 아닌 주문에 접근하면 안 됨
    private void checkPermission(Order order, UUID userId, Role role) {
        if (!isAdminRole(role) && !order.getRequesterUserId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    private boolean isAdminRole(Role role) {
        return Role.MASTER == role || Role.HUB_MANAGER == role;
    }

    private void checkHubPermission(UUID requesterCompanyId, UUID userHubId) {
        try {
            CompanyResponse company = companyServiceClient.getCompany(requesterCompanyId).data();
            if (company == null || !company.hubId().equals(userHubId)) {
                throw new BusinessException(OrderErrorCode.ORDER_HUB_ACCESS_DENIED);
            }
        } catch (FeignException e) {
            throw new BusinessException(OrderErrorCode.COMPANY_SERVICE_UNAVAILABLE);
        }
    }
}
