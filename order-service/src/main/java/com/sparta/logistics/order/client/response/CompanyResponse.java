package com.sparta.logistics.order.client.response;

import java.util.UUID;

public record CompanyResponse(
        UUID companyId,
        String name,
        String type,
        UUID hubId,
        String hubName,
        String address,
        String status
) {}
