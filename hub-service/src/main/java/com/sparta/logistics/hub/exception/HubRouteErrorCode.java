package com.sparta.logistics.hub.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubRouteErrorCode implements ErrorCode {

    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_ROUTE_NOT_FOUND", "허브 간 이동 경로를 찾을 수 없습니다."),
    HUB_ROUTE_ALREADY_EXISTS(HttpStatus.CONFLICT, "HUB_ROUTE_ALREADY_EXISTS", "이미 존재하는 허브 간 이동 경로입니다."),
    HUB_ROUTE_SAME_HUB(HttpStatus.BAD_REQUEST, "HUB_ROUTE_SAME_HUB", "출발 허브와 도착 허브는 같을 수 없습니다."),
    HUB_ROUTE_ALREADY_DELETED(HttpStatus.GONE, "HUB_ROUTE_ALREADY_DELETED", "이미 삭제된 허브 간 이동 경로입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override
    public HttpStatus getStatus() { return status; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
