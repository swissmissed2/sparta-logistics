package com.sparta.logistics.company.client.feign;

import com.sparta.logistics.company.client.model.HubResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/v1/hubs/{hubId}/exists — 허브 존재 여부 확인
 * Company 생성/수정 시 hubId 검증에 사용
 * 주의: Gateway를 통하지 않고 Eureka 서비스명으로 직접 호출 (내부 서비스 간 통신 — 권한 불필요)
 */
@FeignClient(name = "hub-service", fallback = HubFeignClientFallback.class)
public interface HubFeignClient {

    /**
     * 200 OK → 허브 존재
     * 404 Not Found → 허브 없음
     */
    @GetMapping("/api/v1/hubs/{hubId}/exists")
    void checkHubExists(@PathVariable("hubId") UUID hubId);

    /**
     * 단건 허브 이름 조회
     */
    @GetMapping("/{hubId}")
    HubResponse getHub(@PathVariable UUID hubId);

    /**
     * 배치 조회 (목록 조회용 N+1 방지)
     */
    @GetMapping("/api/v1/hubs")
    List<HubResponse> getHubsByIds(@RequestParam("ids") List<UUID> hubIds);

}
