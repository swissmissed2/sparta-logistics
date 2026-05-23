package com.sparta.logistics.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRequest(
        @Size(max = 150)
        String name,

        @Min(value = 0)
        Long price,

        String description,

        String status
) {}
