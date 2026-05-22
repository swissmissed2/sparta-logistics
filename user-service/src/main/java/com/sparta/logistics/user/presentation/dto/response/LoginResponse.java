package com.sparta.logistics.user.presentation.dto.response;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.application.result.UserResult;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import lombok.Builder;

@Builder
public record LoginResponse( // 로그인 응답
        String accessToken,

        String username,

        Role role,

        UserStatus status
) {

    public static LoginResponse from(String accessToken, UserResult userResult) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .username(userResult.username())
                .role(userResult.role())
                .status(userResult.status())
                .build();
    }
}
