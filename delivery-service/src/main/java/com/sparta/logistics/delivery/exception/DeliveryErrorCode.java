package com.sparta.logistics.delivery.exception;

import com.sparta.logistics.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DeliveryErrorCode implements ErrorCode {

    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_001", "배송 정보를 찾을 수 없습니다."),
    DELIVERY_CONFLICT(HttpStatus.CONFLICT, "DELIVERY_002", "다른 요청에 의해 배송 데이터가 변경되었습니다. 다시 시도해 주세요."),
    DELIVERY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "DELIVERY_003", "이미 삭제된 배송입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "DELIVERY_004", "허용되지 않는 배송 상태 전이입니다."),

    MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_MGR_001", "배송담당자를 찾을 수 없습니다."),
    MANAGER_ALREADY_EXISTS(HttpStatus.CONFLICT, "DELIVERY_MGR_002", "이미 등록된 배송담당자입니다."),
    MANAGER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "DELIVERY_MGR_003", "이미 삭제된 배송담당자입니다."),
    MANAGER_IN_DELIVERY(HttpStatus.BAD_REQUEST, "DELIVERY_MGR_004", "배송 중인 담당자는 삭제할 수 없습니다."),

    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_ROUTE_001", "배송경로를 찾을 수 없습니다."),
    INVALID_ROUTE_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "DELIVERY_ROUTE_002", "허용되지 않는 배송경로 상태 전이입니다."),
    DELIVERY_ROUTE_UPDATE_FORBIDDEN(HttpStatus.BAD_REQUEST, "DELIVERY_ROUTE_003", "완료되거나 취소된 배송의 경로는 수정할 수 없습니다."),
    ROUTE_SEQUENCE_VIOLATED(HttpStatus.BAD_REQUEST, "DELIVERY_ROUTE_004", "이전 구간이 완료되지 않아 경로를 업데이트할 수 없습니다."),
    ROUTE_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "DELIVERY_ROUTE_005", "배송 경로 데이터가 누락되었습니다. 운영자 확인이 필요합니다."),

    HUB_NOT_FOUND(HttpStatus.BAD_REQUEST, "DELIVERY_HUB_001", "존재하지 않는 허브입니다."),
    HUB_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "DELIVERY_HUB_002", "Hub Service를 현재 사용할 수 없습니다."),

    USER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "DELIVERY_USER_001", "User Service를 현재 사용할 수 없습니다."),
    ORDER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "DELIVERY_ORDER_001", "Order Service를 현재 사용할 수 없습니다."),

    NO_AVAILABLE_MANAGER(HttpStatus.CONFLICT, "DELIVERY_ASSIGN_001", "배정 가능한 배송 담당자가 없습니다."),
    ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "DELIVERY_ASSIGN_002", "배차 충돌이 반복됩니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
