package com.sparta.logistics.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 토픽: delivery.created
 * 발행: DeliveryService / 구독: OrderService, SlackService(NotificationService)
 * <p>
 * 배송 생성이 완료되면 발행함
 * - OrderService
 *   - p_order_delivery에 deliveryId 누적 저장
 *   - 수신 건수가 totalDeliveryCount에 도달하면 주문 상태를 ACCEPTED로 갱신
 * - SlackService: AI 발송 시한 계산 후 슬랙 알림 발송
 * <p>
 * 파티션 키: deliveryId (배송 단위 이벤트 순서 보장)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCreatedEvent {
    private UUID eventId;
    private UUID deliveryId;
    private UUID orderId;
    private UUID sourceHubId;
    private UUID destinationHubId;
    private String sourceHubName;        // null 가능 (hub-service 조회 실패 시)
    private String destinationHubName;   // null 가능
    private UUID companyDeliveryManagerId;
    private String companyDeliveryManagerSlackId; // null 가능 (미배정 또는 조회 실패 시)
    private Integer totalDeliveryCount;
    private String deliveryAddress;
    private String receiverSlackId;
    private List<DeliveryCreatedItemPayload> orderItems; // null 가능 (product-service 조회 실패 시)
}
