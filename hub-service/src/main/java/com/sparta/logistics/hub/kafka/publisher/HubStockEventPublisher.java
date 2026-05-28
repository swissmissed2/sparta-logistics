package com.sparta.logistics.hub.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.kafka.KafkaTopics;
import com.sparta.logistics.common.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HubStockEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishStockRestoredAck(UUID eventId, UUID orderId) {

        try {
            StockRestoredAckEvent event = StockRestoredAckEvent.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .build();

            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaTopics.STOCK_RESTORED_ACK, orderId.toString(), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[Kafka] stock.restored.ack 발행 실패 - orderId: {}",
                                    orderId, ex);
                        } else {
                            log.info("[Kafka] stock.restored.ack 발행 성공 - orderId: {}",
                                    orderId);
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("[Kafka] stock.restored.ack 발행 실패 - orderId: {}", orderId, e);
        }
    }

    public void publishStockReservationFailed(UUID eventId, UUID orderId, UUID productId, String reason) {

        try {
            StockReservationFailedEvent event = StockReservationFailedEvent.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .productId(productId)
                    .reason(reason)
                    .build();

            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaTopics.STOCK_RESERVATION_FAILED, orderId.toString(), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[Kafka] stock.reservation.failed 발행 실패 - orderId: {}, productId: {}",
                                    orderId, productId, ex);
                        } else {
                            log.info("[Kafka] stock.reservation.failed 발행 성공 - orderId: {}, productId: {}",
                                    orderId, productId);
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("[Kafka] stock.reservation.failed 발행 실패 - orderId: {}", orderId, e);
        }
    }

    public void publishStockReserved(StockReservedEvent event) {

        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaTopics.STOCK_RESERVED, event.getOrderId().toString(), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[Kafka] stock.reserved 발행 실패 - orderId: {}",
                                    event.getOrderId(), ex);
                        } else {
                            log.info("[Kafka] stock.reserved 발행 성공 - orderId: {}",
                                    event.getOrderId());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("[Kafka] stock.reserved 발행 실패 - orderId: {}", event.getOrderId(), e);
        }
    }

    public void publishHubStockUpdated(UUID productId, UUID hubId, Integer available, Long hubStockVersion) {

        try {
            HubStockUpdatedEvent event = HubStockUpdatedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .productId(productId)
                    .hubId(hubId)
                    .available(available)
                    .hubStockVersion(hubStockVersion)
                    .build();

            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaTopics.HUB_STOCK_UPDATED, message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[Kafka] hub.stock.updated 발행 실패 - productId: {}",
                                    productId, ex);
                        } else {
                            log.info("[Kafka] hub.stock.updated 발행 성공 - productId: {}",
                                    productId);
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("[Kafka] hub.stock.updated 발행 실패 - productId: {}", productId, e);
        }
    }

    public void publishStockRestorationFailed(UUID eventId, UUID orderId, String reason) {

        try {
            StockRestorationFailedEvent event = StockRestorationFailedEvent.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .reason(reason)
                    .build();

            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaTopics.STOCK_RESTORATION_FAILED, orderId.toString(), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[Kafka] stock.restoration.failed 발행 실패 - orderId: {}", orderId, ex);
                        } else {
                            log.info("[Kafka] stock.restoration.failed 발행 성공 - orderId: {}", orderId);
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("[Kafka] stock.restoration.failed 발행 실패 - orderId: {}", orderId, e);
        }
    }
}
