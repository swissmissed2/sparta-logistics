package com.sparta.logistics.order.order.dto.response;

import com.sparta.logistics.order.order.entity.Order;
import com.sparta.logistics.order.order.enums.OrderStatus;
import com.sparta.logistics.order.orderitem.dto.response.OrderItemResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class OrderDetailResponse {

    private final UUID orderId;
    private final UUID requesterCompanyId;
    private final String requesterCompanyName;
    private final UUID receiverCompanyId;
    private final String receiverCompanyName;
    private final UUID requesterUserId;
    private final OrderStatus status;
    private final Long totalAmount;
    private final LocalDateTime dueDate;
    private final String requestMemo;
    private final List<OrderItemResponse> orderItems;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private OrderDetailResponse(Order order, String requesterCompanyName, String receiverCompanyName) {
        this.orderId = order.getId();
        this.requesterCompanyId = order.getRequesterCompanyId();
        this.requesterCompanyName = requesterCompanyName;
        this.receiverCompanyId = order.getReceiverCompanyId();
        this.receiverCompanyName = receiverCompanyName;
        this.requesterUserId = order.getRequesterUserId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.dueDate = order.getDueDate();
        this.requestMemo = order.getRequestMemo();
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
    }

    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(order, null, null);
    }

    public static OrderDetailResponse from(Order order, String requesterCompanyName, String receiverCompanyName) {
        return new OrderDetailResponse(order, requesterCompanyName, receiverCompanyName);
    }
}
