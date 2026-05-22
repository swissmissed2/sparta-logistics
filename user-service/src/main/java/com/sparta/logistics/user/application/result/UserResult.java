package com.sparta.logistics.user.application.result;

import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserResult(
        UUID userId,
        String username,
        String name,
        Role role,
        UserStatus status,
        UUID hubId,
        UUID companyId,
        LocalDateTime createdAt
) {
    public static UserResult from(UserEntity user){
        return UserResult.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .status(user.getStatus())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
