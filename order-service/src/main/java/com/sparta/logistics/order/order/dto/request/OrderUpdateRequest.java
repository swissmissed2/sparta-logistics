package com.sparta.logistics.order.order.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderUpdateRequest {

    @Future(message = "납품 기한은 현재 시간 이후여야 합니다.")
    private LocalDateTime dueDate;

    @Size(max = 500, message = "요청 사항은 최대 500자입니다.")
    private String requestMemo;
}
