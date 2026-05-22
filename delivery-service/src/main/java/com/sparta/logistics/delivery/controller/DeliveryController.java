package com.sparta.logistics.delivery.controller;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // TODO: 스웨거
    // 배송 상세 조회
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryDetailResponse> getDelivery(
            @PathVariable UUID deliveryId
    ) {
        DeliveryDetailResponse response = deliveryService.getDelivery(deliveryId);

        return ResponseEntity.ok(response);
    }

    // TODO: 배송 생성 API

    // 배송 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryDetailResponse>>> getDeliveries(
            @PathVariable UUID deliveryId,
            // TODO: AUTH

    ) {
        List<DeliveryListResponse> responses = deliveryService.getDeliveriesByRole(deliveryId, );
        return ResponseEntity.status(HttpStatus.OK)
                .body(responses));
    }
}