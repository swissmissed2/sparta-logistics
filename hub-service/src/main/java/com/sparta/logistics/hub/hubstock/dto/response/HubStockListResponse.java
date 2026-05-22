package com.sparta.logistics.hub.hubstock.dto.response;

import com.sparta.logistics.hub.hubstock.entity.HubStock;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class HubStockListResponse {

    private UUID stockId;
    private UUID hubId;
    private UUID productId;
    private Integer available;
    private Integer reserved;
    private Long version;
    private LocalDateTime updatedAt;

    public static HubStockListResponse from(HubStock hubStock) {
        return HubStockListResponse.builder()
                .stockId(hubStock.getId())
                .hubId(hubStock.getHub().getId())
                .productId(hubStock.getProductId())
                .available(hubStock.getAvailable())
                .reserved(hubStock.getReserved())
                .version(hubStock.getVersion())
                .updatedAt(hubStock.getUpdatedAt())
                .build();
    }
}
