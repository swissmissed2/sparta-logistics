package com.sparta.logistics.user.presentation.dto.response;

import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import com.sparta.logistics.common.domain.Role;
import lombok.Builder;


import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record GetResponse( // 조회 응답
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
    public static GetResponse from(UserEntity user){
        return GetResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .slackId(user.getSlackId())
                .role(user.getRole())
                .status(user.getStatus())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .lastLoginAt(user.getUpdatedAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
