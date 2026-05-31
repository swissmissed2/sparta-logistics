package com.sparta.logistics.delivery.client;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.client.response.HubRouteSegmentResponse;
import com.sparta.logistics.delivery.client.response.UserResponse;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
    private final OrderServiceClient orderServiceClient;

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
        return hubServiceClient.getRouteSegments(sourceHubId, destinationHubId).data();
    }

    public List<HubRouteSegmentResponse> recoverFetchRouteSegments(
        UUID sourceHubId, UUID destinationHubId, Exception e) {
        log.warn("[Feign] hub-service 재시도 초과 — sourceHubId={}, destinationHubId={}",
            sourceHubId, destinationHubId);
        throw new BusinessException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "feign-call", fallbackMethod = "recoverCheckOrderBelongsToCompany")
    public boolean checkOrderBelongsToCompany(UUID orderId, UUID companyId) {
        Boolean result = orderServiceClient.checkOrderBelongsToCompany(orderId, companyId).data();
        return Boolean.TRUE.equals(result);
    }

    public boolean recoverCheckOrderBelongsToCompany(UUID orderId, UUID companyId, Exception e) {
        log.warn("[Feign] order-service 재시도 초과 — orderId={}, companyId={}", orderId, companyId);
        throw new BusinessException(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE);
    }

    @Retry(name = "feign-call", fallbackMethod = "recoverFetchOrderIdsByCompany")
    public List<UUID> fetchOrderIdsByCompany(UUID companyId) {
        List<UUID> result = orderServiceClient.getOrderIdsByCompany(companyId).data();
        return result != null ? result : Collections.emptyList();
    }

    public List<UUID> recoverFetchOrderIdsByCompany(UUID companyId, Exception e) {
        log.warn("[Feign] order-service 재시도 초과 — companyId={}", companyId);
        throw new BusinessException(DeliveryErrorCode.ORDER_SERVICE_UNAVAILABLE);
    }
}