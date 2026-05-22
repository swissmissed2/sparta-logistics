package com.sparta.logistics.hub.hubstock.dto.response;

import com.sparta.logistics.hub.hubstock.entity.HubStock;
import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HubStockAdjustResponse {

    private UUID stockId;
    private Integer available;
    private Integer reserved;
    private Integer changeQuantity;
    private HubStockChangeType changeType;
    private LocalDateTime updatedAt;

    public static HubStockAdjustResponse from(HubStock hubStock, Integer changeQuantity, HubStockChangeType changeType) {
        return HubStockAdjustResponse.builder()
                .stockId(hubStock.getId())
                .available(hubStock.getAvailable())
                .reserved(hubStock.getReserved())
                .changeQuantity(changeQuantity)
                .changeType(changeType)
                .updatedAt(hubStock.getUpdatedAt())
                .build();
    }
}
