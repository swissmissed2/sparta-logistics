package com.sparta.logistics.delivery.controller;

import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.dto.DeliveryListResponse;
import com.sparta.logistics.delivery.dto.DeliverySearchCond;
import com.sparta.logistics.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {
    // TODO: 스웨거
    private final DeliveryService deliveryService;

    // 배송 단건 조회
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryDetailResponse> getDelivery(
            @PathVariable UUID deliveryId
    ) {
        DeliveryDetailResponse response = deliveryService.getDelivery(deliveryId);

        return ResponseEntity.ok(response);
    }

    // 배송 목록 조회
    @GetMapping
    public ResponseEntity<Page<DeliveryListResponse>> getDeliveryList(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-HubId", required = false)  UUID hubId,
            // TODO: page 다시 점검
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @ModelAttribute DeliverySearchCond cond // TODO: 주석제거, 조회용 req dto 객체, modelattribute를 안 붙여도 되지만 명시적으로 표시
    ) {
        // TODO: role == hub manager 면 hub id를 조회해야 한다
        // 현재는 param으로 받아온다

        Page<DeliveryListResponse> response = deliveryService.getDeliveryList(userId, role, hubId, pageable, cond);
        // TODO: 에러 처리 재검토
        return ResponseEntity.ok(response);
    }



    // TODO: 배송 생성

    // TODO: 배송 수정 & 상태 변경

    // TODO: 배송 삭제
}