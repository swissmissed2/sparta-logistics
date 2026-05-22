package com.sparta.logistics.delivery.dto;

import com.sparta.logistics.delivery.entity.DeliveryStatus;

import java.util.UUID;

public record DeliveryCreateResponse(
        UUID id,
        UUID orderId,
        DeliveryStatus status,
        String reason
) {
    // 성공: 오더와 알림에 delivery.created 발행
    public static DeliveryCreateResponse success(UUID orderId, UUID deliveryId) {
        return new DeliveryCreateResponse(deliveryId, orderId, DeliveryStatus.CREATED, null);
    }

    // 실패: 허브와 오더에 delivery.creation.failed 발행
    public static DeliveryCreateResponse fail(UUID orderId, String reason) {
        return new DeliveryCreateResponse(null, orderId, DeliveryStatus.CANCELLED, reason);
    }
}