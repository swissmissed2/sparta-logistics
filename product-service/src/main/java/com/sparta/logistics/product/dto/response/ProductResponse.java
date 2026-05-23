package com.sparta.logistics.product.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        String name,
        UUID companyId,
        String companyName,     // 필요 시 Company FeignClient 조회
        UUID hubId,
        String hubName,
        Long price,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
