package com.sparta.logistics.slack.kafka;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DeliveryCreatedEvent(
    String eventId,
    UUID deliveryId,
    UUID orderId,
    String sourceHubName,
    String destinationHubName,
    String deliveryAddress,
    LocalDateTime dateDate,
    List<OrderItemDto> orderItems,
    String receiverSlackId,
    String companyDeliveryManagerSlackId
) {
  public record OrderItemDto(
      String productName,
      int quantity
  ) {
  }
}
