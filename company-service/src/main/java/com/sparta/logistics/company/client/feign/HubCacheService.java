package com.sparta.logistics.company.client.feign;

import com.sparta.logistics.company.client.model.HubResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 허브 정보(이름) 캐싱
 * - 허브 정보는 변경 빈도가 낮아 캐싱 적용
 * - 17개 허브 고정이므로 캐시 부담 없음
 */
@Service
@RequiredArgsConstructor
public class HubCacheService {

    private final HubFeignClient hubFeignClient;

    @Cacheable(value = "hubs", key = "#hubId")
    public HubResponse getHub(UUID hubId) {
        return hubFeignClient.getHub(hubId);
    }

    // TODO: 허브 정보 변경 시 캐시 무효화(Cache Evict) 처리 필요
    @Cacheable(value = "hubs-batch", key = "#hubIds.toString()")
    public Map<UUID, String> getHubNameMap(List<UUID> hubIds) {
        return hubFeignClient.getHubsByIds(hubIds)
                .stream()
                .collect(Collectors.toMap(HubResponse::hubId, HubResponse::hubName));
    }
}
