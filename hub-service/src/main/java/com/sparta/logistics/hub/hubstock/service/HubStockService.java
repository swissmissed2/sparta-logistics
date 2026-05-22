package com.sparta.logistics.hub.hubstock.service;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.hub.exception.HubErrorCode;
import com.sparta.logistics.hub.exception.HubStockErrorCode;
import com.sparta.logistics.hub.hub.entity.Hub;
import com.sparta.logistics.hub.hub.repository.HubRepository;
import com.sparta.logistics.hub.hubstock.dto.request.AdjustHubStockRequest;
import com.sparta.logistics.hub.hubstock.dto.request.CreateHubStockRequest;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockAdjustResponse;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockCreateResponse;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockListResponse;
import com.sparta.logistics.hub.hubstock.entity.HubStock;
import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import com.sparta.logistics.hub.hubstock.helper.HubStockLockHelper;
import com.sparta.logistics.hub.hubstock.repository.HubStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubStockService {

    private final HubStockRepository hubStockRepository;
    private final HubRepository hubRepository;
    private final HubStockLockHelper hubStockLockHelper;

    private static final int MAX_RETRY = 3;

    // todo: product도 존재 여부를 체크 고민
    @Transactional
    public HubStockCreateResponse createHubStock(UUID hubId, CreateHubStockRequest request) {

        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        // 중복 체크
        if (hubStockRepository.existsByHubAndProductIdAndDeletedAtIsNull(hub, request.getProductId())) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_ALREADY_EXISTS);
        }

        // 레이스 컨디션 처리
        try {
            HubStock hubStock = HubStock.create(hub, request.getProductId(), request.getInitialQuantity());
            HubStock savedStock = hubStockRepository.save(hubStock);
            hubStockRepository.flush();
            return HubStockCreateResponse.from(savedStock);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public Page<HubStockListResponse> getHubStockList(UUID hubId, UUID productId, Pageable pageable) {

        return hubStockRepository.findAllByCondition(hubId, productId, pageable)
                .map(HubStockListResponse::from);
    }

    @Transactional
    public HubStockAdjustResponse adjustHubStock(UUID hubId, UUID stockId, AdjustHubStockRequest request) {

        // changeType 유효성 검증
        if (request.getChangeType() != HubStockChangeType.INBOUND &&
                request.getChangeType() != HubStockChangeType.MANUAL_ADJUST) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_INVALID_CHANGE_TYPE);
        }

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return hubStockLockHelper.adjustWithOptimisticLock(hubId, stockId, request);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRY - 1) {
                    return hubStockLockHelper.adjustWithPessimisticLock(hubId, stockId, request);
                }
            }
        }

        throw new BusinessException(HubStockErrorCode.HUB_STOCK_ADJUST_FAILED);
    }


}
