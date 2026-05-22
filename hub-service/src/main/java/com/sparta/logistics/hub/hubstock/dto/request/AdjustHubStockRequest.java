package com.sparta.logistics.hub.hubstock.dto.request;

import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdjustHubStockRequest {

    @NotNull
    private Integer changeQuantity;

    @NotNull
    private HubStockChangeType changeType;
}
