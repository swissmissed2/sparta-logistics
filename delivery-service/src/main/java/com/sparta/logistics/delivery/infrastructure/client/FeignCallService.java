package com.sparta.logistics.delivery.infrastructure.client;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.client.HubServiceClient;
import com.sparta.logistics.delivery.client.ProductServiceClient;
import com.sparta.logistics.delivery.client.UserServiceClient;
import com.sparta.logistics.delivery.client.response.HubBatchResponse;
import com.sparta.logistics.delivery.client.response.HubRouteSegmentResponse;
import com.sparta.logistics.delivery.client.response.ProductBatchResponse;
import com.sparta.logistics.delivery.client.response.UserResponse;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Feign 호출 전담 컴포넌트 — Resilience4j @Retry 적용.
 *
 * <p>@Retry는 같은 클래스 내 호출 시 AOP 프록시가 동작하지 않으므로
 * DeliveryEventHandler에서 직접 Feign 클라이언트를 호출하는 대신 이 컴포넌트를 통해 호출한다.
 *
 * <p>fallback 파라미터를 {@code Exception}으로 선언한 이유:
 * FeignException(재시도 초과)뿐만 아니라 CB 오픈 시 발생하는
 * CallNotPermittedException도 처리하기 위함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignCallService {

    private final UserServiceClient userServiceClient;
    private final HubServiceClient hubServiceClient;
    private final ProductServiceClient productServiceClient;

    @Retry(name = "feign-call", fallbackMethod = "recoverFetchUser")
    public ApiResponse<UserResponse> fetchUser(UUID receiverId) {
        return userServiceClient.getUser(receiverId);
    }

    public ApiResponse<UserResponse> recoverFetchUser(UUID receiverId, Exception e) {
        log.warn("[Feign] user-service 재시도 초과 — receiverId={}", receiverId);
        throw new BusinessException(DeliveryErrorCode.USER_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "feign-call", fallbackMethod = "recoverFetchRouteSegments")
    public List<HubRouteSegmentResponse> fetchRouteSegments(UUID sourceHubId, UUID destinationHubId) {
        return hubServiceClient.getRouteSegments(sourceHubId, destinationHubId);
    }

    public List<HubRouteSegmentResponse> recoverFetchRouteSegments(
            UUID sourceHubId, UUID destinationHubId, Exception e) {
        log.warn("[Feign] hub-service 재시도 초과 — sourceHubId={}, destinationHubId={}",
                sourceHubId, destinationHubId);
        throw new BusinessException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "feign-call", fallbackMethod = "recoverFetchHubNames")
    public List<HubBatchResponse> fetchHubNames(List<UUID> hubIds) {
        return hubServiceClient.getHubsByIds(hubIds);
    }

    public List<HubBatchResponse> recoverFetchHubNames(List<UUID> hubIds, Exception e) {
        log.warn("[Feign] hub-service batch 재시도 초과 — ids={}", hubIds);
        return List.of();
    }

    @Retry(name = "feign-call", fallbackMethod = "recoverFetchProductNames")
    public List<ProductBatchResponse> fetchProductNames(List<UUID> productIds) {
        return productServiceClient.getProductsByIds(productIds);
    }

    public List<ProductBatchResponse> recoverFetchProductNames(List<UUID> productIds, Exception e) {
        log.warn("[Feign] product-service batch 재시도 초과 — ids={}", productIds);
        return List.of();
    }
}
