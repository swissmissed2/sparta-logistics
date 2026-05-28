package com.sparta.logistics.delivery.client;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.delivery.client.response.HubBatchResponse;
import com.sparta.logistics.delivery.client.response.HubRouteSegmentResponse;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class HubServiceClientFallback implements HubServiceClient {

    @Override
    public void checkHubExists(UUID hubId) {
        log.warn("[HubServiceClient Fallback] Hub Service 응답 없음. hubId={}", hubId);
        throw new BusinessException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
    }

    @Override
    public List<HubRouteSegmentResponse> getRouteSegments(UUID sourceHubId, UUID destinationHubId) {
        log.warn("[HubServiceClient Fallback] Hub Service 응답 없음. sourceHubId={}, destinationHubId={}", sourceHubId, destinationHubId);
        throw new BusinessException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
    }

    @Override
    public List<HubBatchResponse> getHubsByIds(List<UUID> ids) {
        log.warn("[HubServiceClient Fallback] hub-service 응답 없음 — ids={}", ids);
        return List.of();
    }
}
