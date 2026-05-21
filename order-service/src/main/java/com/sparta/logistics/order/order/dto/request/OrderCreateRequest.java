package com.sparta.logistics.order.order.dto.request;

import com.sparta.logistics.order.orderitem.dto.request.OrderItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class OrderCreateRequest {

    @NotNull(message = "요청 업체 ID는 필수입니다.")
    private UUID requesterCompanyId;

    @NotNull(message = "수령 업체 ID는 필수입니다.")
    private UUID receiverCompanyId;

    @NotNull(message = "납품 기한은 필수입니다.")
    @Future(message = "납품 기한은 현재 시간 이후여야 합니다.")
    private LocalDateTime dueDate;

    @Size(max = 500, message = "요청 사항은 최대 500자입니다.")
    private String requestMemo;

    @Valid
    @NotEmpty(message = "주문 상품은 최소 1건 이상이어야 합니다.")
    private List<OrderItemRequest> orderItems;
}
