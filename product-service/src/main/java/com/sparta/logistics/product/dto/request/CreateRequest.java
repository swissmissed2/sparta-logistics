package com.sparta.logistics.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 150, message = "상품명은 최대 150자입니다.")
        String name,

        @NotNull(message = "소속 업체 ID는 필수입니다.")
        UUID companyId,

        @NotNull(message = "관리 허브 ID는 필수입니다.")
        UUID hubId,

        @NotNull(message = "단가는 필수입니다.")
        @Min(value = 0, message = "단가는 0원 이상이어야 합니다.")
        Long price,

        String description
) {}
