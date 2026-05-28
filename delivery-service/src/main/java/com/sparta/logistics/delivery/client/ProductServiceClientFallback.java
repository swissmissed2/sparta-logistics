package com.sparta.logistics.delivery.client;

import com.sparta.logistics.delivery.client.response.ProductBatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public List<ProductBatchResponse> getProductsByIds(List<UUID> ids) {
        log.warn("[ProductServiceClient Fallback] product-service 응답 없음 — ids={}", ids);
        return List.of();
    }
}
