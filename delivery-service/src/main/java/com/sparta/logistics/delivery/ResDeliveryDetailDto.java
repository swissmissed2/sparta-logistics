package com.sparta.logistics.delivery;

import java.util.UUID;

public record ResDeliveryDetailDto(
        UUID id,
        UUID orderId,
        DeliveryStatus status,
        UUID sourceHubId,
        UUID destinationHubId,
        UUID currentHubId,
        String deliveryAddress,
        String receiverSlackId
) {
    public ResDeliveryDetailDto(Delivery delivery) {
        this(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getStatus(),
                delivery.getSourceHubId(),
                delivery.getDestinationHubId(),
                delivery.getCurrentHubId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverSlackId()
        );
    }
}