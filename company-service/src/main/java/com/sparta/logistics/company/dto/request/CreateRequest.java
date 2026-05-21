package com.sparta.logistics.company.dto.request;

import com.sparta.logistics.company.entity.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * POST /api/v1/companies — 업체 생성 요청
 */
public record CreateRequest(
        @NotBlank(message = "업체명은 필수입니다.")
        @Size(max = 100, message = "업체명은 최대 100자입니다.")
        String name,

        @NotNull(message = "업체 타입은 필수입니다.")
        CompanyType type,

        @NotNull(message = "관리 허브 ID는 필수입니다.")
        UUID hubId,

        @NotBlank(message = "업체 주소는 필수입니다.")
        @Size(max = 255, message = "주소는 최대 255자입니다.")
        String address,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        BigDecimal latitude,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        BigDecimal longitude
) {}
