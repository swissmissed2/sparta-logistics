package com.sparta.logistics.delivery.dto;

import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// TODO: 목록 조회 응답 객체 생성
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryListResponse {

    // TODO: 컬럼 재정비
    private UUID deliveryId;
    private UUID orderId;
    private DeliveryStatus status;
    // TODO: name으로 받아오는 거 괜찮은지 다시 Check
    private String sourceHubName;
    private String destinationHubName;
    private String deliveryManagerName;
    private LocalDateTime createdAt;

    // 엔티티를 DTO로 변환
    // TODO: from (delivery, 출발허브명, 도착허브명, 담당자명) 으로 바꾸기
    // 조회 위치 변경
    public static DeliveryListResponse from(DeliveryEntity delivery, String sourceHubName, String destinationHubName, String managerName) {
        return DeliveryListResponse.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus())
                // 서비스에서 조회하는 걸로 변경 완료
                // TODO: 조회 null check
                .sourceHubName(sourceHubName)
                .destinationHubName(destinationHubName)
                .deliveryManagerName(managerName)
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}