package com.sparta.logistics.order.order.controller;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.order.order.dto.request.OrderCancelRequest;
import com.sparta.logistics.order.order.dto.request.OrderCreateRequest;
import com.sparta.logistics.order.order.dto.request.OrderUpdateRequest;
import com.sparta.logistics.order.order.dto.response.OrderDetailResponse;
import com.sparta.logistics.order.order.dto.response.OrderSummaryResponse;
import com.sparta.logistics.order.order.enums.OrderStatus;
import com.sparta.logistics.order.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 주문 생성 **/
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OrderDetailResponse response = orderService.createOrder(
                request.getRequesterCompanyId(),
                request.getReceiverCompanyId(),
                request.getDueDate(),
                request.getRequestMemo(),
                request.getOrderItems(),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("주문이 생성되었습니다.", response));
    }

    /** 주문 목록 조회 **/
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getOrders(
            @RequestParam(required = false) UUID requesterCompanyId,
            @RequestParam(required = false) UUID receiverCompanyId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) LocalDateTime dueDateFrom,
            @RequestParam(required = false) LocalDateTime dueDateTo,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        Page<OrderSummaryResponse> response = orderService.getOrders(requesterCompanyId, receiverCompanyId, status, dueDateFrom, dueDateTo, role, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** 주문 단건 조회 **/
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        OrderDetailResponse response = orderService.getOrder(orderId, userId, role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** 주문 수정 **/
    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        OrderDetailResponse response = orderService.updateOrder(orderId, request.getDueDate(), request.getRequestMemo(), userId, role);
        return ResponseEntity.ok(ApiResponse.ok("주문이 수정되었습니다.", response));
    }

    /** 주문 취소 **/
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @PathVariable UUID orderId,
            @RequestBody OrderCancelRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        OrderDetailResponse response = orderService.cancelOrder(orderId, request.getCancelReason(), userId, role);
        return ResponseEntity.ok(ApiResponse.ok("주문이 취소되었습니다.", response));
    }
}
