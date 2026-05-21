package com.sparta.logistics.company.dto.request;

import com.sparta.logistics.company.entity.CompanyStatus;
import com.sparta.logistics.company.entity.CompanyType;

import java.util.UUID;

/**
 * GET 검색 파라미터
 */
public record SearchCondition(
        String name,
        CompanyType type,
        UUID hubId,
        CompanyStatus status
) {}
