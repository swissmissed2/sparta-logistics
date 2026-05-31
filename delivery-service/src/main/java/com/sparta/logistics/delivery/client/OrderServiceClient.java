package com.sparta.logistics.delivery.client;

import com.sparta.logistics.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "order-service", fallback = OrderServiceClientFallback.class)
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{orderId}/company-check")
    ApiResponse<Boolean> checkOrderBelongsToCompany(
            @PathVariable("orderId") UUID orderId,
            @RequestParam("companyId") UUID companyId
    );

    @GetMapping("/api/v1/orders/by-company")
    ApiResponse<List<UUID>> getOrderIdsByCompany(@RequestParam("companyId") UUID companyId);
}
