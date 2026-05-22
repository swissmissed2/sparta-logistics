package com.sparta.logistics.user.presentation.dto.response;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ApproveResponse( // 승인 및 거절 응답
        UUID userId,
        String username,
        UserStatus status,
        Role role
) {

    public static ApproveResponse from(UserEntity user){
        return ApproveResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .role(user.getRole())
                .build();
    }
}
