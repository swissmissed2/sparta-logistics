package com.sparta.logistics.user.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // AUTH
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.","AUTH-001"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 사용자명입니다.", "AUTH-002"),
    PASSWORD_NOT_MATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.","AUTH-003"),
    USER_NOT_APPROVED(HttpStatus.FORBIDDEN,"승인되지 않은 사용자입니다.","AUTH-004"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.","AUTH-005"),
    ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 사용자 상태입니다.","AUTH-006"),


    // USER
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 요청에 대한 접근 권한이 없습니다.", "USER-001");

    private final HttpStatus status;
    private final String message;
    private final String code;
}
