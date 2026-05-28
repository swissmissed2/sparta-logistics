package com.sparta.logistics.delivery.client;

import com.sparta.logistics.delivery.client.response.HubBatchResponse;
import com.sparta.logistics.delivery.client.response.HubRouteSegmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

// hub-service 팀과 엔드포인트 경로 협의 필요 — company-service 패턴 동일 적용
@FeignClient(name = "hub-service", fallback = HubServiceClientFallback.class)
public interface HubServiceClient {

    // 200 OK → 허브 존재 / 404 Not Found → 허브 없음 (Fallback에서 예외 처리)
    @GetMapping("/api/v1/hubs/{hubId}/exists")
    void checkHubExists(@PathVariable("hubId") UUID hubId);

    @GetMapping("/api/v1/hub-routes/segments")
    List<HubRouteSegmentResponse> getRouteSegments(
            @RequestParam("sourceHubId") UUID sourceHubId,
            @RequestParam("destinationHubId") UUID destinationHubId
    );

    @GetMapping("/api/v1/hubs/batch")
    List<HubBatchResponse> getHubsByIds(@RequestParam("ids") List<UUID> ids);
}
