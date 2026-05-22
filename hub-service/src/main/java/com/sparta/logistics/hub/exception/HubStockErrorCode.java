package com.sparta.logistics.hub.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubStockErrorCode implements ErrorCode {

    HUB_STOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "HUB_STOCK_001", "이미 존재하는 허브 재고입니다."),
    HUB_STOCK_INVALID_CHANGE_TYPE(HttpStatus.BAD_REQUEST, "HUB_STOCK_002", "유효하지 않은 변경 유형입니다."),
    HUB_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_STOCK_003", "허브 재고를 찾을 수 없습니다."),
    HUB_STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "HUB_STOCK_004", "재고가 부족합니다."),
    HUB_STOCK_ADJUST_FAILED(HttpStatus.CONFLICT, "HUB_STOCK_005", "재고 조정에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
