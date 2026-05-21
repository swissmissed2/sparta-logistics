package com.sparta.logistics.hub.hubroute.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateHubRouteRequest {

    @NotNull
    private UUID sourceHubId;

    @NotNull
    private UUID destinationHubId;

    @NotNull
    @DecimalMin(value = "0.001", message = "이동 거리는 0보다 커야 합니다.")
    private BigDecimal distance;

    @NotNull
    @Min(value = 1, message = "소요 시간은 1분 이상이어야 합니다.")
    private Integer duration;
}
