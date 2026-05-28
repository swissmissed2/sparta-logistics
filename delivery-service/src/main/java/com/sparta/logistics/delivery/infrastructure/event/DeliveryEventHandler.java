package com.sparta.logistics.delivery.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.kafka.KafkaTopics;
import com.sparta.logistics.common.kafka.event.AiDeadlineCalculatedEvent;
import com.sparta.logistics.common.kafka.event.CancelDeliveryCommand;
import com.sparta.logistics.common.kafka.event.RestoreStockItemPayload;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.client.response.HubRouteSegmentResponse;
import com.sparta.logistics.delivery.client.response.UserResponse;
import com.sparta.logistics.delivery.dto.event.StockReservedEventDto;
import com.sparta.logistics.delivery.dto.event.StockReservedItemPayload;
import com.sparta.logistics.delivery.infrastructure.client.FeignCallService;
import com.sparta.logistics.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventHandler {

    private final DeliveryService deliveryService;
    private final DeliveryEventPublisher eventPublisher;
    private final FeignCallService feignCallService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.STOCK_RESERVED, groupId = "delivery-service")
    public void handleStockReserved(
            String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        StockReservedEventDto event;
        try {
            event = objectMapper.readValue(message, StockReservedEventDto.class);
        } catch (JsonProcessingException e) {
            // 역직렬화 실패는 재시도해도 의미 없음 — 의도적 offset 커밋
            // orderId 추출 불가 → publishCreationFailed 미호출 (보상 불가, 수동 처리 필요)
            log.error("[Kafka][수동처리 필요] stock.reserved 역직렬화 실패 — topic={}, partition={}, offset={}",
                    topic, partition, offset, e);
            return;
        }

        // hub-service가 이벤트 레벨 sourceHubId를 발행하지 않으므로 items[0]에서 파생
        // 허브당 1이벤트 구조 전제 — 모든 items의 sourceHubId가 동일함
        UUID sourceHubId = event.sourceHubId();
        if (sourceHubId == null && !event.orderItems().isEmpty()) {
            sourceHubId = event.orderItems().get(0).sourceHubId();
        }

        if (sourceHubId == null || event.destinationHubId() == null) {
            log.warn("[Kafka] 허브 ID null — orderId={}", event.orderId());
            eventPublisher.publishCreationFailed(event.orderId(), null, "INVALID_HUB_ID",
                    toRestoreItems(event.orderItems()));
            return;
        }

        final UUID resolvedSourceHubId = sourceHubId;
        // sourceHubId가 이벤트 레벨에 없을 경우 파생값으로 새 DTO 재구성
        if (event.sourceHubId() == null) {
            event = new StockReservedEventDto(
                    event.orderId(), event.receiverId(), resolvedSourceHubId,
                    event.destinationHubId(), event.deliveryAddress(),
                    event.orderItems(), event.totalDeliveryCount()
            );
        }

        // user-service Feign 호출 — 3회 retry 후 실패 시 BusinessException (CB 내부 예외 포함)
        ApiResponse<UserResponse> userResponse;
        try {
            userResponse = feignCallService.fetchUser(event.receiverId());
        } catch (Exception e) {
            log.warn("[Kafka] user-service 호출 실패 — orderId={}", event.orderId());
            eventPublisher.publishCreationFailed(event.orderId(), null, "USER_SERVICE_UNAVAILABLE",
                    toRestoreItems(event.orderItems()));
            return;
        }

        if (userResponse.data() == null) {
            log.warn("[Kafka] slackId 없음(data=null) — receiverId={}, orderId={}", event.receiverId(), event.orderId());
            eventPublisher.publishCreationFailed(event.orderId(), null, "SLACK_ID_NOT_FOUND",
                    toRestoreItems(event.orderItems()));
            return;
        }

        String slackId = userResponse.data().slackId();
        if (slackId == null) {
            log.warn("[Kafka] slackId 없음 — orderId={}", event.orderId());
            eventPublisher.publishCreationFailed(event.orderId(), null, "SLACK_ID_NOT_FOUND",
                    toRestoreItems(event.orderItems()));
            return;
        }

        // hub-service Feign 호출 — 3회 retry 후 실패 시 BusinessException (CB 내부 예외 포함)
        List<HubRouteSegmentResponse> routeSegments;
        try {
            routeSegments = feignCallService.fetchRouteSegments(event.sourceHubId(), event.destinationHubId());
        } catch (Exception e) {
            log.warn("[Kafka] hub-service 호출 실패 — orderId={}", event.orderId());
            eventPublisher.publishCreationFailed(event.orderId(), null, "HUB_SERVICE_UNAVAILABLE",
                    toRestoreItems(event.orderItems()));
            return;
        }

        try {
            deliveryService.createDelivery(event, slackId, routeSegments);
            log.info("[Kafka] 배송 생성 완료 — orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("[Kafka] 배송 생성 실패 — orderId={}", event.orderId(), e);
            eventPublisher.publishCreationFailed(event.orderId(), null, "CREATE_FAILED",
                    toRestoreItems(event.orderItems()));
        }
    }

    @KafkaListener(topics = KafkaTopics.CANCEL_DELIVERY_COMMAND, groupId = "delivery-service")
    public void handleCancelDeliveryCommand(String message) {
        CancelDeliveryCommand command;
        try {
            command = objectMapper.readValue(message, CancelDeliveryCommand.class);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] cancel.delivery.command 역직렬화 실패: {}", message, e);
            return;
        }

        try {
            boolean cancelled = deliveryService.cancelDeliveryByCommand(command.getDeliveryId());
            if (cancelled) {
                eventPublisher.publishCancelledAck(command.getDeliveryId(), command.getOrderId());
            } else {
                eventPublisher.publishCancellationFailed(command.getOrderId(), command.getDeliveryId(), "DELIVERY_IN_TRANSIT");
            }
        } catch (Exception e) {
            log.error("[Kafka] 배송 취소 처리 실패 — orderId={}", command.getOrderId(), e);
            eventPublisher.publishCancellationFailed(command.getOrderId(), command.getDeliveryId(), "CANCEL_FAILED");
        }
    }

    @KafkaListener(topics = KafkaTopics.AI_DEADLINE_CALCULATED, groupId = "delivery-service")
    public void handleAiDeadlineCalculated(String message) {
        AiDeadlineCalculatedEvent event;
        try {
            event = objectMapper.readValue(message, AiDeadlineCalculatedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] ai.deadline.calculated 역직렬화 실패: {}", message, e);
            return;  // 보상 액션 없음 — offset 커밋
        }
        try {
            deliveryService.updateFinalDispatchDeadline(event.getDeliveryId(), event.getFinalDispatchDeadlineAt());
            log.info("[Kafka] AI 발송 시한 업데이트 — deliveryId={}", event.getDeliveryId());
        } catch (BusinessException e) {
            // DELIVERY_NOT_FOUND 등 재처리해도 해결 안 됨 — offset 커밋
            log.error("[Kafka] AI 발송 시한 업데이트 실패(비즈니스) — deliveryId={}", event.getDeliveryId(), e);
        } catch (Exception e) {
            // KafkaException 등 일시적 장애 — DefaultErrorHandler 재시도
            log.error("[Kafka] AI 발송 시한 업데이트 실패 — deliveryId={}", event.getDeliveryId(), e);
            throw new RuntimeException(e);
        }
    }

    private List<RestoreStockItemPayload> toRestoreItems(List<StockReservedItemPayload> items) {
        if (items == null) return Collections.emptyList();
        return items.stream()
                .map(i -> RestoreStockItemPayload.builder()
                        .productId(i.productId())
                        .hubId(i.sourceHubId())
                        .quantity(i.reservedQuantity())
                        .build())
                .toList();
    }
}
