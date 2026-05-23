package com.sparta.logistics.delivery.dto;

import com.sparta.logistics.delivery.entity.DeliveryStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeliverySearchCond {
    // TODO: field check
    private UUID orderId;
    private DeliveryStatus status;
    private UUID sourceHubId;
    private UUID destinationHubId;
    private UUID deliveryManagerId;

    // Role에 의해 강제 필터링될 필드
    private UUID authorizedHubId;
    private UUID authorizedManagerId;
}
