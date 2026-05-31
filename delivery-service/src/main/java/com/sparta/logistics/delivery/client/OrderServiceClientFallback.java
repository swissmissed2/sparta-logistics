package com.sparta.logistics.delivery.client;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public ApiResponse<Boolean> checkOrderBelongsToCompany(UUID orderId, UUID companyId) {
        log.warn("[OrderServiceClient Fallback] Order Service 응답 없음. orderId={}, companyId={}", orderId, companyId);
        throw new BusinessException(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE);
    }

    @Override
    public ApiResponse<List<UUID>> getOrderIdsByCompany(UUID companyId) {
        log.warn("[OrderServiceClient Fallback] Order Service 응답 없음. companyId={}", companyId);
        throw new BusinessException(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE);
    }
}
