package com.sparta.logistics.user.presentation.dto.response;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.domain.model.enums.UserStatus;

public record LoginResponse( // 로그인 응답
        String accessToken,

        String username,

        Role role,

        UserStatus status
) {
}
