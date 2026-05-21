package com.sparta.logistics.user.presentation.dto.request;

import com.sparta.logistics.common.domain.Role;

import java.util.UUID;

public record UpdateRequest(
        String name,
        String email,
        String slackId,
        Role role,
        UUID hubId,
        UUID companyId
) {
}
