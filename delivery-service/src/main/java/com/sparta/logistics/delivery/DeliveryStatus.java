package com.sparta.logistics.delivery;

public enum DeliveryStatus {
    CREATED,    // 배송 생성 (초기 상태)
    HUB_WAITING, // 허브 대기 중
    HUB_MOVING, // 허브 이동 중
    DESTINATION_HUB_ARRIVED, // 목적지 허브 도착
    OUT_FOR_DELIVERY, // 업체 이동 중
    COMPLETED,  // 배송 완료
    CANCELED    // 취소
}