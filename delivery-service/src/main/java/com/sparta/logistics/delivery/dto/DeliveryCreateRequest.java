package com.sparta.logistics.delivery.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class DeliveryCreateRequest {
    // TODO: 주문 식별자 필드 정의
    private UUID orderId;

    // TODO: 사용자 식별자 필드 정의
    private UUID userId;

    // TODO: 배송지 주소 필드 정의
    private String address;
}
