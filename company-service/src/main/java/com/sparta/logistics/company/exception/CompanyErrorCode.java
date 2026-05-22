package com.sparta.logistics.company.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CompanyErrorCode implements ErrorCode {


    // 400대
    COMPANY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "COMPANY_001", "이미 삭제된 업체입니다."),
    HUB_NOT_FOUND(HttpStatus.BAD_REQUEST, "COMPANY_002", "존재하지 않는 허브입니다."),
    INVALID_COMPANY_TYPE(HttpStatus.BAD_REQUEST, "COMPANY_003", "유효하지 않은 업체 타입입니다."),
    INVALID_COMPANY_STATUS(HttpStatus.BAD_REQUEST, "COMPANY_004", "유효하지 않은 업체 상태입니다."),

    // 403대
    COMPANY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMPANY_403", "해당 업체에 대한 접근 권한이 없습니다."),

    // 404대
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_001", "업체를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
