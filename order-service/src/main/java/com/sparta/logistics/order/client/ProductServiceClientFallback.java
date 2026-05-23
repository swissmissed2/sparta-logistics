package com.sparta.logistics.order.client;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.order.client.response.ProductResponse;
import com.sparta.logistics.order.exception.OrderErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public ApiResponse<ProductResponse> getProduct(UUID productId) {
        log.warn("[ProductServiceClient Fallback] Product Service 응답 없음. productId={}", productId);
        throw new BusinessException(OrderErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
    }
}
