package com.sparta.logistics.hub.hubstock.helper;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.hub.exception.HubStockErrorCode;
import com.sparta.logistics.hub.hubstock.dto.request.AdjustHubStockRequest;
import com.sparta.logistics.hub.hubstock.dto.response.HubStockAdjustResponse;
import com.sparta.logistics.hub.hubstock.entity.HubStock;
import com.sparta.logistics.hub.hubstock.repository.HubStockRepository;
import com.sparta.logistics.hub.hubstocklog.entity.HubStockLog;
import com.sparta.logistics.hub.hubstocklog.repository.HubStockLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubStockLockHelper {

    private final HubStockRepository hubStockRepository;
    private final HubStockLogRepository hubStockLogRepository;

    // 새 트랜잭션 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HubStockAdjustResponse adjustWithOptimisticLock(UUID hubId, UUID stockId, AdjustHubStockRequest request) {

        HubStock hubStock = hubStockRepository.findByIdAndDeletedAtIsNull(stockId)
                .orElseThrow(() -> new BusinessException(HubStockErrorCode.HUB_STOCK_NOT_FOUND));

        return adjust(hubStock, hubId, request);
    }

    // 새 트랜잭션 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HubStockAdjustResponse adjustWithPessimisticLock(UUID hubId, UUID stockId, AdjustHubStockRequest request) {

        HubStock hubStock = hubStockRepository.findByIdWithLock(stockId)
                .orElseThrow(() -> new BusinessException(HubStockErrorCode.HUB_STOCK_NOT_FOUND));

        return adjust(hubStock, hubId, request);
    }

    private HubStockAdjustResponse adjust(HubStock hubStock, UUID hubId, AdjustHubStockRequest request) {

        if (!hubStock.getHub().getId().equals(hubId)) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_NOT_FOUND);
        }
        if (hubStock.getAvailable() + request.getChangeQuantity() < 0) {
            throw new BusinessException(HubStockErrorCode.HUB_STOCK_INSUFFICIENT);
        }

        // 재고 변경 이력 생성
        int changeQuantity = request.getChangeQuantity();
        int beforeQuantity = hubStock.getAvailable();
        int afterQuantity = hubStock.getAvailable() + changeQuantity;
        hubStockLogRepository.save(HubStockLog.create(
                hubStock,
                changeQuantity,
                beforeQuantity,
                afterQuantity,
                request.getChangeType()
        ));

        hubStock.adjustAvailable(request.getChangeQuantity());
        return HubStockAdjustResponse.from(hubStock, request.getChangeQuantity(), request.getChangeType());
    }
}
