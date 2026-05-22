package com.sparta.logistics.company.client.feign;

import com.sparta.logistics.company.client.model.HubExistsResponse;
import com.sparta.logistics.company.client.model.HubResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Hub Service 장애 시 Circuit Breaker 대신 기본 Fallback 구현
 * - 허브 존재 확인 실패 시 허브 없음으로 처리 (안전 우선)
 * - Hub 조회 실패 시 시스템 안정성을 위해 "알 수 없음"으로 대체 응답 반환
 * - 단건/다건 조회 모두 안전한 기본값으로 degrade(성능/기능 축소 운영) 처리
 */
@Slf4j
@Component
public class HubFeignClientFallback implements HubFeignClient {

    @Override
    public HubExistsResponse checkHubExists(UUID hubId) {
        log.warn("[HubFeignClient Fallback] Hub Service 응답 없음. hubId={}", hubId);
        // Fallback: 서킷 오픈 시 허브가 없는 것으로 처리 → 주문 보호
        return new HubExistsResponse(false);
    }

    @Override
    public HubResponse getHub(UUID hubId) {
        log.warn("[HubFeignClient Fallback] Hub Service 응답 없음. hubId={}", hubId);
        return new HubResponse(hubId, "알 수 없음");
    }

    @Override
    public List<HubResponse> getHubsByIds(List<UUID> hubIds) {
        log.warn("[HubFeignClient Fallback] Hub Service 응답 없음. hubIds={}", hubIds);
        return hubIds.stream()
                .map(id -> new HubResponse(id, "알 수 없음"))
                .toList();
    }
}
