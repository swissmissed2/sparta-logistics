package com.sparta.logistics.order.orderitem.dto.response;

import com.sparta.logistics.order.orderitem.entity.OrderItem;
import lombok.Getter;

import java.util.UUID;

@Getter
public class OrderItemResponse {

    private final UUID orderItemId;
    private final UUID productId;
    private final String productName;
    private final Long unitPrice;
    private final Integer quantity;
    private final Long subTotal;

    private OrderItemResponse(OrderItem item) {
        this.orderItemId = item.getId();
        this.productId = item.getProductId();
        this.productName = item.getProductName();
        this.unitPrice = item.getUnitPrice();
        this.quantity = item.getQuantity();
        this.subTotal = item.getSubTotal();
    }

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item);
    }
}
