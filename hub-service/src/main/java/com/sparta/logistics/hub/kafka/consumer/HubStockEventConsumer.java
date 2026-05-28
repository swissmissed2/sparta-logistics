package com.sparta.logistics.hub.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.kafka.KafkaTopics;
import com.sparta.logistics.common.kafka.event.DeliveryCreationFailedEvent;
import com.sparta.logistics.common.kafka.event.DeliveryStartedEvent;
import com.sparta.logistics.common.kafka.event.OrderCreatedEvent;
import com.sparta.logistics.common.kafka.event.RestoreStockCommand;
import com.sparta.logistics.hub.hubstock.service.HubStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubStockEventConsumer {

    private final ObjectMapper objectMapper;
    private final HubStockService hubStockService;

    /**
     * restore.stock.command 구독 — 주문 취소 시 재고 복구 (Orchestration 보상)
     */
    @KafkaListener(topics = KafkaTopics.RESTORE_STOCK_COMMAND, groupId = "hub-service")
    public void consumeRestoreStockCommand(String message) {

        try {
            RestoreStockCommand command = objectMapper.readValue(message, RestoreStockCommand.class);

            log.info("[Kafka] restore.stock.command 수신 - orderId: {}", command.getOrderId());

            hubStockService.restoreStock(command);
        } catch (JsonProcessingException e) {
            // 역직렬화 실패 — 재시도 의미 없으므로 의도적 스킵
            log.error("[Kafka] restore.stock.command 역직렬화 실패 - 스킵 처리: {}", message, e);
        }
    }

    /**
     * delivery.creation.failed 구독 — 배송 생성 실패 시 재고 복구 (Choreography 보상)
     */
    @KafkaListener(topics = KafkaTopics.DELIVERY_CREATION_FAILED, groupId = "hub-service")
    public void consumeDeliveryCreationFailed(String message) {

        try {
            DeliveryCreationFailedEvent event = objectMapper
                    .readValue(message, DeliveryCreationFailedEvent.class);

            log.info("[Kafka] delivery.creation.failed 수신 - orderId: {}", event.getOrderId());

            hubStockService.restoreOnDeliveryFailed(event);
        } catch (JsonProcessingException e) {
            // 역직렬화 실패 — 재시도 의미 없으므로 의도적 스킵
            log.error("[Kafka] delivery.creation.failed 역직렬화 실패 - message: {}", message, e);
        }
    }

    /**
     * order.created 구독 — 주문 생성 시 재고 예약
     */
    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "hub-service")
    public void consumeOrderCreated(String message) {

        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            log.info("[Kafka] order.created 수신 - orderId: {}", event.getOrderId());

            hubStockService.reserveStock(event);
        } catch (JsonProcessingException e) {
            // 역직렬화 실패 — 재시도 의미 없으므로 의도적 스킵
            log.error("[Kafka] order.created 역직렬화 실패 - message: {}", message, e);
        }
    }

    /**
     * delivery.started 구독 — 배송 시작 시 예약 재고 최종 차감
     */
    @KafkaListener(topics = KafkaTopics.DELIVERY_STARTED, groupId = "hub-service")
    public void consumeDeliveryStarted(String message) {

        try {
            DeliveryStartedEvent event = objectMapper
                    .readValue(message, DeliveryStartedEvent.class);

            log.info("[Kafka] delivery.started 수신 - orderId: {}", event.getOrderId());

            hubStockService.deductReservedStock(event);
        } catch (JsonProcessingException e) {
            // 역직렬화 실패 — 재시도 의미 없으므로 의도적 스킵
            log.error("[Kafka] delivery.started 역직렬화 실패 - message: {}", message, e);
        }
    }
}
