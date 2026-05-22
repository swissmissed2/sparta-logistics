package com.sparta.logistics.company.dto.request;

import java.util.UUID;

/**
 * GET 검색 파라미터
 */
public record SearchCondition(
        String name,
        String type,
        UUID hubId,
        String status
) {}
