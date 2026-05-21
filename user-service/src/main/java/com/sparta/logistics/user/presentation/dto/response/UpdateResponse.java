package com.sparta.logistics.user.presentation.dto.response;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.domain.model.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateResponse( // 수정 응답
        UUID userId,
        String username,
        String name,
        String email,
        String slackId,
        Role role,
        UserStatus status,
        UUID hubId,
        UUID companyId,
        LocalDateTime updateAt
) {
}
