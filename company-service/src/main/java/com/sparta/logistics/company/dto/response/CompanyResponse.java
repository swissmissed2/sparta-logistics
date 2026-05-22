package com.sparta.logistics.company.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CompanyResponse(
        UUID companyId,
        String name,
        String type,
        UUID hubId,
        String hubName, // Hub Service에서 이름 조회 가능하나 SA 문서상 간접참조이므로 별도 처리
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
