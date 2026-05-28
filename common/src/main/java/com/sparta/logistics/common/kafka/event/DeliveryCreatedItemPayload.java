package com.sparta.logistics.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** DeliveryCreatedEvent 내 배송 상품 항목 페이로드 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCreatedItemPayload {
    private String productName;
    private Integer quantity;
}
