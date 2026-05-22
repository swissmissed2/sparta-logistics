package com.sparta.logistics.delivery.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.entity.Delivery;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    // 배송 단건 조회
    // 마스터&업체 매니저: 모든 배송 건
    // 허브 매니저: 담당 허브의 배송 건만
    // 배송 매니저: 본인 배송 건만
    @Transactional(readOnly = true)
    public DeliveryDetailResponse getDelivery(UUID deliveryId, UUID userId, Role role) {
        Delivery delivery = findDelivery(deliveryId);

        // TODO: 권한 확인
        //checkPermission(delivery, userId, role);

        return DeliveryDetailResponse.from(delivery);

    }

    // 배송 단건 조회
    private Delivery findDelivery(UUID deliveryId) {
        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
    }


    // 배송 목록 조회

    // 배송 수정

    // 배송 상태 수정

    // 배송 삭제

    // 배송 생성

}