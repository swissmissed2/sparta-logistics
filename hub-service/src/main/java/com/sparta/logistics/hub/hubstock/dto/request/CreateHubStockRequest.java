package com.sparta.logistics.hub.hubstock.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateHubStockRequest {

    @NotNull
    private UUID productId;

    @NotNull
    @Min(1)
    private Integer initialQuantity;
}
