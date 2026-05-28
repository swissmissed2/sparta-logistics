package com.sparta.logistics.gateway.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sparta.logistics.gateway.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data
) {
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, 400, errorCode.getMessage(), null);
    }
}
