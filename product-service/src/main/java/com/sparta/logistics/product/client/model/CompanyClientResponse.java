package com.sparta.logistics.product.client.model;

import java.util.UUID;

public record CompanyClientResponse(
        UUID companyId,
        String name,
        String type,
        UUID hubId,
        String hubName,
        String address,
        String status
) {}
