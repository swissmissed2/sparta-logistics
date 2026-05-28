package com.sparta.logistics.gateway.exception;

import com.sparta.logistics.gateway.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum JwtErrorCode implements ErrorCode {
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "GW_JWT_001", "인증 토큰이 누락되었습니다."),
    INCORRECT_TOKEN(HttpStatus.UNAUTHORIZED, "GW_JWT_002", "토큰 인증에 실패했습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "GW_JWT_003", "토큰 유효 기간이 만료되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GW_JWT_500", "게이트웨이 인증 시스템에 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
