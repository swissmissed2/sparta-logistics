package com.sparta.logistics.company.dto.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PUT /api/v1/companies/{companyId} — 업체 수정 요청
 * 모든 필드 선택적 (null이면 변경 안 함)
 */
public record UpdateRequest(
        @Size(max = 100)
        String name,

        String type,

        UUID hubId,

        @Size(max = 255)
        String address,

        BigDecimal latitude,
        BigDecimal longitude,

        String status
) {}
