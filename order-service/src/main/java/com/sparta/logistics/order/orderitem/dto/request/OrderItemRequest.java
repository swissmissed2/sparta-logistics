package com.sparta.logistics.order.orderitem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class OrderItemRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private UUID productId;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 최소 1 이상이어야 합니다.")
    private Integer quantity;
}
