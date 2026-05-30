package com.sparta.logistics.delivery.dto;

import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.enums.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryListResponse {

    private UUID deliveryId;
    private UUID orderId;
    private DeliveryStatus status;
    private String sourceHubName;       // 허브명 미지원 시 hubId.toString() 임시 사용
    private String destinationHubName;  // 허브명 미지원 시 hubId.toString() 임시 사용
    private String deliveryManagerName; // SA 기준: companyDeliveryManagerId (허브명 조회 API 연동 전까지 ID 문자열)
    private LocalDateTime createdAt;

    public static DeliveryListResponse from(DeliveryEntity delivery, String sourceHubName,
                                            String destinationHubName, String managerName) {
        return DeliveryListResponse.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus())
                .sourceHubName(sourceHubName)
                .destinationHubName(destinationHubName)
                .deliveryManagerName(managerName)
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}