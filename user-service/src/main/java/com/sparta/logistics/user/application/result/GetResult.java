package com.sparta.logistics.user.application.result;

import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import lombok.Builder;

import com.sparta.logistics.common.domain.Role;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record GetResult(
        UUID userId,
        String username,
        String name,
        String email,
        String slackId,
        Role role,
        UserStatus status,
        UUID hubId,
        UUID companyId,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static GetResult from(UserEntity user){
        return GetResult.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .slackId(user.getSlackId())
                .role(user.getRole())
                .status(user.getStatus())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .lastLoginAt(user.getLast_login_at())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
