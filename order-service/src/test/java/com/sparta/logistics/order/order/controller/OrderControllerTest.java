package com.sparta.logistics.order.order.controller;

import com.sparta.logistics.common.config.CommonSecurityConfig;
import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.order.order.dto.response.OrderDetailResponse;
import com.sparta.logistics.order.order.dto.response.OrderSummaryResponse;
import com.sparta.logistics.order.order.enums.OrderStatus;
import com.sparta.logistics.order.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(CommonSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    // ===== POST /api/v1/orders =====

    // 유효한 주문 생성 요청 시 201 응답과 success:true가 반환되는지 검증
    @Test
    void createOrder_validRequest_returns201() throws Exception {
        OrderDetailResponse response = mock(OrderDetailResponse.class);
        when(response.getOrderId()).thenReturn(ORDER_ID);
        when(orderService.createOrder(any(), any(), any(), any(), any(), any())).thenReturn(response);

        String body = """
                {
                    "requesterCompanyId": "%s",
                    "receiverCompanyId": "%s",
                    "dueDate": "%s",
                    "requestMemo": "테스트 메모",
                    "orderItems": [{ "productId": "%s", "quantity": 2 }]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(7), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-User-Id", USER_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201));
    }

    // requesterCompanyId 누락 시 @Valid 검증 실패로 400이 반환되는지 검증
    @Test
    void createOrder_missingRequesterCompanyId_returns400() throws Exception {
        String body = """
                {
                    "receiverCompanyId": "%s",
                    "dueDate": "%s",
                    "orderItems": [{ "productId": "%s", "quantity": 1 }]
                }
                """.formatted(UUID.randomUUID(), LocalDateTime.now().plusDays(7), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-User-Id", USER_ID.toString()))
                .andExpect(status().isBadRequest());
    }

    // orderItems가 빈 배열이면 @NotEmpty 검증 실패로 400이 반환되는지 검증
    @Test
    void createOrder_emptyOrderItems_returns400() throws Exception {
        String body = """
                {
                    "requesterCompanyId": "%s",
                    "receiverCompanyId": "%s",
                    "dueDate": "%s",
                    "orderItems": []
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(7));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-User-Id", USER_ID.toString()))
                .andExpect(status().isBadRequest());
    }

    // ===== GET /api/v1/orders =====

    // 유효한 주문 목록 조회 요청 시 200 응답과 success:true가 반환되는지 검증
    @Test
    void getOrders_validRequest_returns200() throws Exception {
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(orderService.getOrders(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", Role.MASTER.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // 필터 파라미터(requesterCompanyId, status)가 서비스에 올바르게 전달되는지 검증
    @Test
    void getOrders_withFilters_callsServiceWithCorrectParams() throws Exception {
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(orderService.getOrders(eq(COMPANY_ID), any(), eq(OrderStatus.PENDING), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/orders")
                        .param("requesterCompanyId", COMPANY_ID.toString())
                        .param("status", "PENDING")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", Role.HUB_MANAGER.name()))
                .andExpect(status().isOk());

        verify(orderService).getOrders(eq(COMPANY_ID), any(), eq(OrderStatus.PENDING), any(), any(), any(), any(), any());
    }

    // ===== GET /api/v1/orders/{orderId} =====

    // 유효한 단건 주문 조회 요청 시 200 응답과 success:true가 반환되는지 검증
    @Test
    void getOrder_validRequest_returns200() throws Exception {
        OrderDetailResponse response = mock(OrderDetailResponse.class);
        when(response.getOrderId()).thenReturn(ORDER_ID);
        when(orderService.getOrder(eq(ORDER_ID), eq(USER_ID), eq(Role.MASTER))).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", Role.MASTER.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ===== PUT /api/v1/orders/{orderId} =====

    // 유효한 주문 수정 요청 시 200 응답과 수정 완료 메시지가 반환되는지 검증
    @Test
    void updateOrder_validRequest_returns200WithMessage() throws Exception {
        OrderDetailResponse response = mock(OrderDetailResponse.class);
        when(orderService.updateOrder(eq(ORDER_ID), any(), any(), eq(USER_ID), eq(Role.MASTER), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/orders/{orderId}", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "requestMemo": "수정된 메모" }
                                """)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", Role.MASTER.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문이 수정되었습니다."));
    }

    // ===== DELETE /api/v1/orders/{orderId} =====

    // 유효한 주문 취소 요청 시 200 응답과 취소 완료 메시지가 반환되는지 검증
    @Test
    void cancelOrder_validRequest_returns200WithMessage() throws Exception {
        OrderDetailResponse response = mock(OrderDetailResponse.class);
        when(orderService.cancelOrder(eq(ORDER_ID), any(), eq(USER_ID), eq(Role.MASTER), any()))
                .thenReturn(response);

        mockMvc.perform(delete("/api/v1/orders/{orderId}", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "cancelReason": "단순 변심" }
                                """)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", Role.MASTER.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문이 취소되었습니다."));
    }

    // HUB_MANAGER가 X-User-HubId 헤더와 함께 취소 요청 시 hubId가 서비스에 올바르게 전달되는지 검증
    @Test
    void cancelOrder_hubManagerWithHubIdHeader_success() throws Exception {
        UUID hubId = UUID.randomUUID();
        OrderDetailResponse response = mock(OrderDetailResponse.class);
        when(orderService.cancelOrder(eq(ORDER_ID), eq("재고 소진"), eq(USER_ID), eq(Role.HUB_MANAGER), eq(hubId)))
                .thenReturn(response);

        mockMvc.perform(delete("/api/v1/orders/{orderId}", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "cancelReason": "재고 소진" }
                                """)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", Role.HUB_MANAGER.name())
                        .header("X-User-HubId", hubId.toString()))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(eq(ORDER_ID), eq("재고 소진"), eq(USER_ID), eq(Role.HUB_MANAGER), eq(hubId));
    }
}
