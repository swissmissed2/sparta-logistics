package com.sparta.logistics.hub.hubstock.dto.response;

import com.sparta.logistics.hub.hubstock.entity.HubStock;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HubStockCreateResponse {

    private UUID stockId;
    private UUID hubId;
    private UUID productId;
    private Integer available;
    private Integer reserved;
    private LocalDateTime createdAt;

    public static HubStockCreateResponse from(HubStock hubStock) {
        return HubStockCreateResponse.builder()
                .stockId(hubStock.getId())
                .hubId(hubStock.getHub().getId())
                .productId(hubStock.getProductId())
                .available(hubStock.getAvailable())
                .reserved(hubStock.getReserved())
                .createdAt(hubStock.getCreatedAt())
                .build();
    }
}
