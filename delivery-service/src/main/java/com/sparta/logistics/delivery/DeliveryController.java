package com.sparta.logistics.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // 단건 조회
    @GetMapping("/{id}")
    public ResDeliveryDetailDto getDelivery(@PathVariable UUID id) {
        return deliveryService.getDeliveryList(id);
    }

}
