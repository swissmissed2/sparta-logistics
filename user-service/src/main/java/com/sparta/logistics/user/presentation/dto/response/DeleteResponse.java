package com.sparta.logistics.user.presentation.dto.response;

import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DeleteResponse( // 삭제 응답
        UUID userId,
        String username,
        UserStatus status,
        LocalDateTime deleteAt
) {
    public static DeleteResponse from(UserEntity user) {
        return DeleteResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .deleteAt(user.getDeletedAt())
                .build();
    }

}
