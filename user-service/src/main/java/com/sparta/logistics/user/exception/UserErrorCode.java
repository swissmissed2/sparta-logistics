package com.sparta.logistics.user.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED, "AUTH-001"),
    INVALID_REFRESH_TOKEN("유효하지 않은 Refresh Token입니다.", HttpStatus.UNAUTHORIZED, "AUTH-002"),
    ACCESS_DENIED("해당 요청에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN, "AUTH-003"),
    PASSWORD_NOT_MATCH("비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED, "AUTH-004"),

    USER_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND, "USER-001"),
    USER_ALREADY_EXISTS("이미 사용 중인 사용자명입니다.", HttpStatus.CONFLICT, "USER-002"),
    EMAIL_ALREADY_EXISTS("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT, "USER-003"),
    USER_NOT_APPROVED("승인되지 않은 사용자입니다.", HttpStatus.FORBIDDEN, "USER-004"),
    ALREADY_PROCESSED("이미 처리된 사용자 상태입니다.", HttpStatus.BAD_REQUEST, "USER-005"),
    INVALID_ROLE_CONSTRAINT("역할에 맞지 않는 정보가 포함되어 있습니다.", HttpStatus.BAD_REQUEST, "USER-006"),

    INVALID_PAGE_SIZE("페이지 크기는 10, 30, 50만 허용됩니다.", HttpStatus.BAD_REQUEST, "USER-007"),
    HUB_ID_REQUIRED("HUB_MANAGER 또는 DELIVERY_MANAGER는 hubId가 필수입니다.", HttpStatus.BAD_REQUEST, "USER-008"),
    COMPANY_ID_REQUIRED("COMPANY_MANAGER는 companyId가 필수입니다.", HttpStatus.BAD_REQUEST, "USER-009"),
    MASTER_CANNOT_HAVE_HUB_OR_COMPANY("MASTER는 hubId와 companyId를 입력할 수 없습니다.", HttpStatus.BAD_REQUEST, "USER-010"),
    COMPANY_ID_NOT_ALLOWED("허브 담당자 또는 배송 담당자는 업체 ID를 가질 수 없습니다.", HttpStatus.BAD_REQUEST, "USER-011"),
    HUB_ID_NOT_ALLOWED("업체 담당자는 허브 ID를 가질 수 없습니다.", HttpStatus.BAD_REQUEST, "USER-012"),

    HUB_NOT_FOUND("존재하지 않는 허브입니다.", HttpStatus.NOT_FOUND, "USER-HUB-001"),
    HUB_MISMATCH("다른 허브 소속 사용자입니다.", HttpStatus.FORBIDDEN, "USER-HUB-002"),
    HUB_SERVICE_UNAVAILABLE("허브 서비스에 연결할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE, "USER-HUB-003"),

    COMPANY_NOT_FOUND("존재하지 않는 업체입니다.", HttpStatus.NOT_FOUND, "USER-COMPANY-001"),
    COMPANY_SERVICE_UNAVAILABLE("업체 서비스에 연결할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE, "USER-COMPANY-002");

    private final String message;
    private final HttpStatus status;
    private final String code;
}
