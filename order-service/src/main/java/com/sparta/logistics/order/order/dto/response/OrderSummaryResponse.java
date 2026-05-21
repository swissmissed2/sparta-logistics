package com.sparta.logistics.order.order.dto.response;

import com.sparta.logistics.order.order.entity.Order;
import com.sparta.logistics.order.order.enums.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class OrderSummaryResponse {

    private final UUID orderId;
    private final UUID requesterCompanyId;
    private final UUID receiverCompanyId;
    private final OrderStatus status;
    private final Long totalAmount;
    private final LocalDateTime dueDate;
    private final LocalDateTime createdAt;

    private OrderSummaryResponse(Order order) {
        this.orderId = order.getId();
        this.requesterCompanyId = order.getRequesterCompanyId();
        this.receiverCompanyId = order.getReceiverCompanyId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.dueDate = order.getDueDate();
        this.createdAt = order.getCreatedAt();
    }

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order);
    }
}
