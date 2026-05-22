package com.sparta.logistics.delivery.controller;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // 배송 단건 조회
    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryDetailResponse>> getDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        DeliveryDetailResponse response = deliveryService.getDelivery(deliveryId, userId, role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
