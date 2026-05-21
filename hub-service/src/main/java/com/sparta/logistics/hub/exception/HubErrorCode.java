package com.sparta.logistics.hub.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubErrorCode implements ErrorCode {

    HUB_NAME_DUPLICATED(HttpStatus.CONFLICT, "HUB_002", "이미 존재하는 허브명입니다."),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_001", "허브를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
