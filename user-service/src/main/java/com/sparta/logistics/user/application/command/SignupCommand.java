package com.sparta.logistics.user.application.command;

import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.common.domain.Role;
import lombok.Builder;

import java.util.UUID;

@Builder
public record SignupCommand(
        String username,
        String password,
        String name,
        String email,
        String slackId,
        Role role,
        UUID hubId,
        UUID companyId

) {

    public UserEntity toEntity(String encodedPassword) {
        return UserEntity.builder()
                .username(username)
                .password(encodedPassword)
                .name(name)
                .email(email)
                .slackId(slackId)
                .role(role)
                .hubId(hubId)
                .companyId(companyId)
                .build();

    }
}
