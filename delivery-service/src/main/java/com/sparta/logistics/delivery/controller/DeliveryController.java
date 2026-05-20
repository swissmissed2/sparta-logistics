package com.sparta.logistics.delivery.controller;

import com.sparta.logistics.delivery.service.DeliveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryDetailResponse> getDeliveryDetail(
            @PathVariable UUID deliveryId
    ) {
        // TODO: 서비스 호출 코드 위치 표시
        throw new UnsupportedOperationException("구현 필요");
    }
}
