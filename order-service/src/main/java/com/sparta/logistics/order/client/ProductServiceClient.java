package com.sparta.logistics.order.client;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.order.client.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service", fallback = ProductServiceClientFallback.class)
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/{productId}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("productId") UUID productId);
}
