package com.sparta.logistics.order.order.enums;

public enum OrderStatus {
    PENDING,     // 주문 접수
    ACCEPTED,    // 주문 승인
    IN_DELIVERY, // 배송 중
    COMPLETED,   // 주문 완료
    CANCELLED    // 주문 취소
}
