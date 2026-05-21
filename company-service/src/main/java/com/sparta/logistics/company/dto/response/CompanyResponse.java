package com.sparta.logistics.company.dto.response;

import com.sparta.logistics.company.entity.Company;
import com.sparta.logistics.company.entity.CompanyStatus;
import com.sparta.logistics.company.entity.CompanyType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CompanyResponse(
        UUID companyId,
        String name,
        CompanyType type,
        UUID hubId,
        String hubName, // Hub Service에서 이름 조회 가능하나 SA 문서상 간접참조이므로 별도 처리
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        CompanyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** Entity → Response 변환
     * - from: hubName 없이 변환 (Fallback용)
     * - of: hubName 포함 변환
     */
    public static CompanyResponse from(Company company) {
        return CompanyResponse.builder()
                .companyId(company.getId())
                .name(company.getName())
                .type(company.getType())
                .hubId(company.getHubId())
                .hubName(null)
                .address(company.getAddress())
                .latitude(company.getLatitude())
                .longitude(company.getLongitude())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    public static CompanyResponse of(Company company, String hubName) {
        return CompanyResponse.builder()
                .companyId(company.getId())
                .name(company.getName())
                .type(company.getType())
                .hubId(company.getHubId())
                .hubName(hubName)
                .address(company.getAddress())
                .latitude(company.getLatitude())
                .longitude(company.getLongitude())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
