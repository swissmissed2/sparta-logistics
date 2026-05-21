package com.sparta.logistics.delivery.dto;

import com.sparta.logistics.delivery.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class DeliveryCreateResponse {
    // TODO: 배송 식별자 필드 정의
    private UUID id;

    // TODO: 주문 식별자 필드 정의
    private UUID orderId;

    // TODO: 사용자 식별자 필드 정의
    private UUID userId;

    // TODO: 배송지 주소 필드 정의
    private String address;

    // TODO: 배송 상태 필드 정의
    private DeliveryStatus status;
}
