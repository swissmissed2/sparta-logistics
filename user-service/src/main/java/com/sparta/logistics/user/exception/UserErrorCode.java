package com.sparta.logistics.user.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("존재하지 않는 사용자입니다.",HttpStatus.NOT_FOUND,"USER-001"),
    USER_ALREADY_EXISTS("이미 사용 중인 사용자명입니다.",HttpStatus.CONFLICT,  "USER-002"),
    PASSWORD_NOT_MATCH("비밀번호가 일치하지 않습니다.",HttpStatus.UNAUTHORIZED, "USER-003"),
    USER_NOT_APPROVED("승인되지 않은 사용자입니다.",HttpStatus.FORBIDDEN,"USER-004"),
    INVALID_TOKEN("유효하지 않은 토큰입니다.",HttpStatus.UNAUTHORIZED, "USER-005"),
    ALREADY_PROCESSED("이미 처리된 사용자 상태입니다.",HttpStatus.BAD_REQUEST, "USER-006"),
    ACCESS_DENIED("해당 요청에 대한 접근 권한이 없습니다.",HttpStatus.FORBIDDEN,  "USER-007"),
    INVALID_ROLE_CONSTRAINT("역할에 맞지 않는 정보가 포함되어 있습니다.",HttpStatus.BAD_REQUEST, "USER-008"),
    INVALID_PAGE_SIZE("페이지 크기는 10, 30, 50만 허용됩니다.", HttpStatus.BAD_REQUEST, "USER-009"),
    HUB_MISMATCH("다른 허브 소속 사용자입니다.", HttpStatus.FORBIDDEN, "USER-010");


    private final String message;
    private final HttpStatus status;
    private final String code;
}
