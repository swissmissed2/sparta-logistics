package com.sparta.logistics.user.application.result;

public record Token(
        UserResult userResult,
        String accessToken,
        String refreshToken
) {
}
