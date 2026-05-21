package com.sparta.logistics.order.client.response;

import java.util.UUID;

public record ProductResponse(
        UUID productId,
        String name,
        UUID companyId,
        String companyName,
        UUID hubId,
        String hubName,
        Long price,
        String description,
        String status
) {}
