package com.sparta.logistics.hub.hub.service;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.hub.exception.HubErrorCode;
import com.sparta.logistics.hub.hub.dto.request.CreateHubRequest;
import com.sparta.logistics.hub.hub.dto.request.UpdateHubRequest;
import com.sparta.logistics.hub.hub.dto.response.*;
import com.sparta.logistics.hub.hub.entity.Hub;
import com.sparta.logistics.hub.hub.enums.HubStatus;
import com.sparta.logistics.hub.hub.repository.HubRepository;
import com.sparta.logistics.hub.hubroute.entity.HubRoute;
import com.sparta.logistics.hub.hubroute.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;


    @CacheEvict(value = "hubList", allEntries = true)
    @Transactional
    public HubCreateResponse createHub(CreateHubRequest request) {

        // 허브 이름 중복 체크
        if (hubRepository.existsByName(request.getName())) {
            throw new BusinessException(HubErrorCode.HUB_NAME_DUPLICATED);
        }

        Hub hub = Hub.create(
                request.getName(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude()
        );

        // 동시 요청으로 인한 레이스 컨디션 처리
        try {
            Hub savedHub = hubRepository.save(hub);
            hubRepository.flush();
            return HubCreateResponse.from(savedHub);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HubErrorCode.HUB_NAME_DUPLICATED);
        }
    }

    @Cacheable(
            value = "hubList",
            key = "#name +" +
                    " ':' + #address +" +
                    " ':' + #status +" +
                    " ':' + #pageable.pageNumber +" +
                    " ':' + #pageable.pageSize +" +
                    " ':' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public Page<HubListResponse> getHubList(String name, String address, HubStatus status, Pageable pageable) {

        return hubRepository.findAllByCondition(name, address, status, pageable)
                .map(HubListResponse::from);
    }

    @Cacheable(value = "hubs", key = "#hubId")
    @Transactional(readOnly = true)
    public HubDetailResponse getHub(UUID hubId) {

        return HubDetailResponse.from(findByHubId(hubId));
    }

    @Transactional(readOnly = true)
    public boolean existsHub(UUID hubId) {

        return hubRepository.existsByIdAndDeletedAtIsNull(hubId);
    }

    @Caching(evict = {
            @CacheEvict(value = "hubs", key = "#hubId"),
            @CacheEvict(value = "hubList", allEntries = true)
    })
    @Transactional
    public HubUpdateResponse updateHub(UUID hubId, UpdateHubRequest request) {

        Hub hub = findByHubId(hubId);

        // 허브 이름 중복 본인 제외 체크
        if (hubRepository.existsByNameAndIdNot(request.getName(), hubId)) {
            throw new BusinessException(HubErrorCode.HUB_NAME_DUPLICATED);
        }

        hub.update(
                request.getName(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.getStatus()
        );

        // 동시 요청으로 인한 레이스 컨디션 처리
        try {
            hubRepository.flush();
            return HubUpdateResponse.from(hub);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HubErrorCode.HUB_NAME_DUPLICATED);
        }
    }

    // todo: 배송 담당자 논리 삭제 연동
    @Caching(evict = {
            @CacheEvict(value = "hubs", key = "#hubId"),
            @CacheEvict(value = "hubList", allEntries = true)
    })
    @Transactional
    public HubDeleteResponse deleteHub(UUID hubId, UUID userId) {

        Hub hub = findByHubId(hubId);
        hub.delete(userId);

        List<HubRoute> routes = hubRouteRepository.findAllByHubAndDeletedAtIsNull(hub);
        routes.forEach(route -> route.delete(userId));

        return HubDeleteResponse.from(hub);
    }

    private Hub findByHubId(UUID hubId) {

        return hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
    }
}
