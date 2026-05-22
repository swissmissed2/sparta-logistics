package com.sparta.logistics.product.dto.request;

import java.util.UUID;

public record SearchCondition(
        String name,
        UUID companyId,
        UUID hubId,
        String status
) {}
