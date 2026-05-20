package com.sparta.logistics.delivery.entity;

public enum DeliveryStatus {
    CREATED,
    HUB_WAITING,
    HUB_MOVING,
    DESTINATION_HUB_ARRIVED,
    OUT_FOR_DELIVERY,
    COMPLETED,
    CANCELLED;
}
