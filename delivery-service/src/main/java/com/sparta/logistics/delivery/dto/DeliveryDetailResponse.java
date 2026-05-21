package com.sparta.logistics.delivery.dto;

import com.sparta.logistics.delivery.entity.Delivery;
import com.sparta.logistics.delivery.entity.DeliveryStatus;

import java.util.UUID;

public record DeliveryDetailResponse(
        UUID deliveryId,
        DeliveryStatus status,
        String deliveryAddress
) {
    // 엔티티 TO DTO
    public static DeliveryDetailResponse from(Delivery deliveryEntity) {
        return new DeliveryDetailResponse(
                deliveryEntity.getId(),
                deliveryEntity.getStatus(),
                deliveryEntity.getDeliveryAddress()
        );
    }
}