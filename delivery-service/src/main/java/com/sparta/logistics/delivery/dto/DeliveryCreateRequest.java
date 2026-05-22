package com.sparta.logistics.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Hub에서 발행한 stock.reserved 구독
public record DeliveryCreateRequest (
        @NotNull UUID orderId,
        @NotBlank String deliveryAddress,
        @NotNull UUID receiverId,
        String receiverSlackId
) {
}