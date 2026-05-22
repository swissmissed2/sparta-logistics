package com.sparta.logistics.order.order.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.order.client.CompanyServiceClient;
import com.sparta.logistics.order.client.ProductServiceClient;
import com.sparta.logistics.order.client.response.CompanyResponse;
import com.sparta.logistics.order.client.response.ProductResponse;
import com.sparta.logistics.order.exception.OrderErrorCode;
import com.sparta.logistics.order.order.dto.response.OrderDetailResponse;
import com.sparta.logistics.order.order.dto.response.OrderSummaryResponse;
import com.sparta.logistics.order.order.entity.Order;
import com.sparta.logistics.order.order.enums.OrderStatus;
import com.sparta.logistics.order.order.repository.OrderRepository;
import com.sparta.logistics.order.orderitem.dto.request.OrderItemRequest;
import com.sparta.logistics.order.orderitem.entity.OrderItem;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CompanyServiceClient companyServiceClient;
    private final ProductServiceClient productServiceClient;

    /**
     * 주문 생성
     * 1. requesterCompanyId / receiverCompanyId 존재 여부 검증 (Company Service) ✅
     * 2. 각 상품 정보(이름·단가) 조회 (Product Service) ✅
     * 3. Order 및 OrderItem 생성 (PENDING) ✅
     * 4. Hub Service에 재고 예약 요청 // TODO: Kafka Choreography Saga
     * 5. Delivery Service에서 배송 및 경로 자동 생성 // TODO: Kafka Choreography Saga
     * 6. Notification Service에서 AI 발송 시간 계산 후 슬랙 알림 발송 // TODO: Kafka Choreography Saga
     * */
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
        validateCompanyExists(requesterCompanyId);
        validateCompanyExists(receiverCompanyId);

        Order order = Order.create(requesterCompanyId, receiverCompanyId, userId, dueDate, requestMemo);

        items.forEach(item -> {
            ProductResponse product = fetchProduct(item.getProductId());
            OrderItem orderItem = OrderItem.create(
                    order,
                    item.getProductId(),
                    product.name(),
                    product.price(),
                    item.getQuantity()
            );
            order.addOrderItem(orderItem);
        });

        order.calculateTotalAmount();
        orderRepository.save(order);
        return OrderDetailResponse.from(order);
    }

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
    }

    /**
     * 주문 취소
     * 1. Delivery Service: 배송 및 경로 취소 // TODO: Kafka Orchestration Saga
     * 2. Hub Service: 재고 복구 // TODO: Kafka Orchestration Saga
     * 3. 주문 상태 → CANCELLED ✅
     * */
    @Transactional
    public OrderDetailResponse cancelOrder(UUID orderId, String cancelReason, UUID userId, Role role, UUID userHubId) {
        // 취소는 HUB_MANAGER 또는 MASTER만 가능
        if (!isAdminRole(role)) {
            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_PERMISSION_DENIED);
        }

        Order order = findOrder(orderId);

        // HUB_MANAGER는 본인 담당 허브 소속 업체의 주문만 취소 가능
        if (role == Role.HUB_MANAGER) {
            checkHubPermission(order.getRequesterCompanyId(), userHubId);
        }

        // CANCELLED, COMPLETED, IN_DELIVERY 상태는 취소 불가
        if (!order.isModifiable()) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
        }

        order.cancel(userId, cancelReason);
        return OrderDetailResponse.from(order);
    }

    private void validateCompanyExists(UUID companyId) {
        try {
            companyServiceClient.checkCompanyExists(companyId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorCode.COMPANY_NOT_FOUND);
        } catch (FeignException e) {
            throw new BusinessException(OrderErrorCode.COMPANY_SERVICE_UNAVAILABLE);
        }
    }

    private ProductResponse fetchProduct(UUID productId) {
        try {
            // AVAILABLE 상태가 아닌 상품은 주문 불가
            ProductResponse product = productServiceClient.getProduct(productId).data();
            if (!"AVAILABLE".equals(product.status())) {
                throw new BusinessException(OrderErrorCode.PRODUCT_NOT_AVAILABLE);
            }
            return product;
        } catch (FeignException.NotFound e) {
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
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
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
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
