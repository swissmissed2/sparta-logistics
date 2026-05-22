package com.sparta.logistics.user.presentation.dto.response;

import com.sparta.logistics.user.application.result.UserResult;
import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SignupResponse( // 회원가입 응답
        UUID userId,
        String username,
        String name,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static SignupResponse from(UserResult userResult) {
        return SignupResponse.builder()
                .userId(userResult.userId())
                .username(userResult.username())
                .name(userResult.name())
                .role(userResult.role())
                .status(userResult.status())
                .createdAt(userResult.createdAt())
                .build();
    }
}
