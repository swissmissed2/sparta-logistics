package com.sparta.logistics.order.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001", "주문을 찾을 수 없습니다."),
    ORDER_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "ORDER_002", "완료 또는 취소된 주문은 수정할 수 없습니다."),
    ORDER_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "ORDER_003", "완료 또는 취소된 주문은 취소할 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER_004", "해당 주문에 대한 접근 권한이 없습니다."),
    ORDER_CANCEL_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "ORDER_005", "주문 취소 권한이 없습니다."),
    ORDER_UPDATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "ORDER_006", "주문 수정 권한이 없습니다."),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_007", "업체를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_008", "상품을 찾을 수 없습니다."),
    COMPANY_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_009", "업체 서비스 연결에 실패했습니다."),
    PRODUCT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_010", "상품 서비스 연결에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}