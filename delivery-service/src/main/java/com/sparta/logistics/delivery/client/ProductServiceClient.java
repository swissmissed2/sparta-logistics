package com.sparta.logistics.delivery.client;

import com.sparta.logistics.delivery.client.response.ProductBatchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-service", fallback = ProductServiceClientFallback.class)
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/batch")
    List<ProductBatchResponse> getProductsByIds(@RequestParam("ids") List<UUID> ids);
}
