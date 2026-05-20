package com.sparta.logistics.delivery.service;

import com.sparta.logistics.delivery.entity.DeliveryRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public <DeliveryDetailResponse> getDeliveryDetail(UUID deliveryId) {
        // TODO: 입력 파라미터 검증 위치 표시
        // TODO: Delivery 조회 위치 표시
        // TODO: 예외 처리 위치 표시
        // TODO: DeliveryResponseDto 반환 위치 표시
        throw new UnsupportedOperationException("구현 필요");
    }
}