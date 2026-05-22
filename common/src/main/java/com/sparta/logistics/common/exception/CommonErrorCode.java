package com.sparta.logistics.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode{

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON_001", "유효성 검증에 실패했습니다."),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "COMMON_002", "요청 파라미터 타입이 올바르지 않습니다."),
    MISSING_REQUEST_HEADER(HttpStatus.BAD_REQUEST, "COMMON_003", "필수 헤더가 누락되었습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "COMMON_004", "요청 바디를 읽을 수 없습니다."),


    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
