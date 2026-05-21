package com.sparta.logistics.company.common.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CompanyErrorCode implements ErrorCode {

    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_001", "업체를 찾을 수 없습니다."),
    COMPANY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "COMPANY_002", "이미 삭제된 업체입니다."),
    HUB_NOT_FOUND(HttpStatus.BAD_REQUEST, "COMPANY_003", "존재하지 않는 허브입니다."),
    COMPANY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMPANY_004", "해당 업체에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
