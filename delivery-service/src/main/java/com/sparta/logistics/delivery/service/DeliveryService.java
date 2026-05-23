package com.sparta.logistics.delivery.service;

import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.dto.DeliveryListResponse;
import com.sparta.logistics.delivery.dto.DeliverySearchCond;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    // 배송 단건 조회
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

    // 배송 목록 조회
    @Transactional(readOnly = true)
    public Page<DeliveryListResponse> getDeliveryList(UUID userId, String role, UUID hubId, Pageable pageable, DeliverySearchCond cond) {
        // 1. 기본 조건 설정
        // TODO: 조회용 DTO 객체 생성 or 재사용 결정

        // 2. 권한별
        // TODO: 로직 점검

        switch (role) {
            case "HUB_MANAGER" -> {
                // 이 유저가 관리하는 허브 ID를 가져와서 조회용 DTO 객체에 주입
                // 해야 하는데, hub service를 gateway를 통해 접근할 예정이라 Controller에서 받아옴
                // UUID myHubId = hubService.findHubIdByManagerId(userId);
                // TODO: 주석 제거, Controller에서 잘 보내주는지 check
                // TODO: hubId가 NULL인 상황 막았는지 check
                cond.setAuthorizedHubId(hubId);
            }
            case "DELIVERY_MANAGER" -> {
                cond.setAuthorizedManagerId(userId);
            }
            case "MASTER", "COMPANY_MANAGER" -> {
                // 전체 조회
            }
            default -> {} // TODO: 에러 코드
        }
        // TODO: 주석 제거, Repository 호출해서 Entity 가져옴
        // delivery page에서 sourceHubId, destinationHubId, deliveryManagerId 수집
        // null과 중복 제거
        // hub service에서 조회, manager에서 조회해서 ID->이름 맵을 제작
        // map에서 각 Delivery의 ID로 이름을 찾아서 DTO에 넘기자
        Page<DeliveryEntity> deliveryPage = deliveryRepository.findAllByCondition(cond, pageable);

        // TODO: 결정하기 1. UUID 말고 객체로 두기 (MSA에서 비권장일 것 같음)
        // TODO: 결정하기 2. ID만 노출하고 클라이언트가 다시 다른 서비스 호출해서 name 조회
        // TODO: 결정하기 3. entity에 hubname을 그냥 저장 (반정규화)

        // TODO: 주석 제거, Page<Entity>를 Page<DTO>로 변환
        // TODO: 로직 결정 시 null 값 수정 (null check)
        return deliveryPage.map(d -> DeliveryListResponse.from(
                d,
                null, // sourceHubName
                null, // destinationHubName
                null  // deliveryManagerName
        ));
    }
}