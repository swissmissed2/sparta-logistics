package com.sparta.logistics.hub.hub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CreateHubRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String address;

    @NotNull
    @DecimalMin("-90.000000") @DecimalMax("90.000000")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin("-180.000000") @DecimalMax("180.000000")
    private BigDecimal longitude;
}
