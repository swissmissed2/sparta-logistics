package com.sparta.logistics.hub.hubstocklog.dto.response;

import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import com.sparta.logistics.hub.hubstocklog.entity.HubStockLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HubStockLogListResponse {

    private UUID logId;
    private UUID hubStockId;
    private UUID orderItemId;
    private UUID deliveryId;
    private Integer changeQuantity;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private HubStockChangeType changeType;
    private LocalDateTime createdAt;
    private UUID createdBy;

    public static HubStockLogListResponse from(HubStockLog log, UUID stockId) {
        return HubStockLogListResponse.builder()
                .logId(log.getId())
                .hubStockId(stockId)
                .orderItemId(log.getOrderItemId())
                .deliveryId(log.getDeliveryId())
                .changeQuantity(log.getChangeQuantity())
                .beforeQuantity(log.getBeforeQuantity())
                .afterQuantity(log.getAfterQuantity())
                .changeType(log.getChangeType())
                .createdAt(log.getCreatedAt())
                .createdBy(log.getCreatedBy())
                .build();
    }
}
