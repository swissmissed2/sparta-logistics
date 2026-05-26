package com.sparta.logistics.hub.hubstocklog.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.hub.exception.HubErrorCode;
import com.sparta.logistics.hub.exception.HubStockErrorCode;
import com.sparta.logistics.hub.hub.repository.HubRepository;
import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import com.sparta.logistics.hub.hubstock.repository.HubStockRepository;
import com.sparta.logistics.hub.hubstocklog.dto.response.HubStockLogListResponse;
import com.sparta.logistics.hub.hubstocklog.repository.HubStockLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubStockLogService {

    private final HubStockLogRepository hubStockLogRepository;
    private final HubRepository hubRepository;
    private final HubStockRepository hubStockRepository;

    @Transactional(readOnly = true)
    public Page<HubStockLogListResponse> getHubStockLogList(
            UUID hubId, UUID stockId, HubStockChangeType changeType, Pageable pageable, Role role, UUID userHubId) {

        // 권한 검증
        checkHubStockPermission(role, userHubId, hubId);

        // 허브 존재 여부 검증
        if (hubRepository.existsByIdAndDeletedAtIsNull(hubId)) {
            throw new BusinessException(HubErrorCode.HUB_NOT_FOUND);
        }

        // 허브-재고 존재 여부 검증
        if (!hubStockRepository.existsByIdAndDeletedAtIsNull(stockId)) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_NOT_FOUND);
        }

        return hubStockLogRepository.findAllByCondition(stockId, hubId, changeType, pageable)
                .map(log -> HubStockLogListResponse.from(log, stockId));
    }

    private void checkHubStockPermission(Role role, UUID userHubId, UUID hubId) {

        if (role == Role.MASTER) return;

        if (role == Role.HUB_MANAGER) {
            if (userHubId == null || !userHubId.equals(hubId)) {
                throw new BusinessException(HubStockErrorCode.HUB_STOCK_FORBIDDEN);
            }
            return;
        }

        throw new BusinessException(HubStockErrorCode.HUB_STOCK_FORBIDDEN);
    }
}
