package com.sparta.logistics.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DeliveryStartedEvent 내 배송 출고 대상 상품 항목 페이로드
 * orderItemId 제거: DeliveryOrderItemEntity에 orderItemId가 없어 null 발행되던 문제 해소
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrderItemPayload {
    private UUID productId;
    private UUID hubId;
    private Integer quantity;
}
