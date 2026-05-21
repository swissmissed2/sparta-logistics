package com.sparta.logistics.delivery.service;

import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Transactional(readOnly = true)
    public DeliveryDetailResponse getDelivery(UUID deliveryId) {

        // TODO: 공통 예외 처리 통일
        if (deliveryId == null) {
            throw new IllegalArgumentException("배송 ID는 필수 입력 값입니다.");
        }

        DeliveryEntity deliveryEntity = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 배송 정보가 존재하지 않습니다. ID: " + deliveryId));

        return DeliveryDetailResponse.from(deliveryEntity);
    }
}