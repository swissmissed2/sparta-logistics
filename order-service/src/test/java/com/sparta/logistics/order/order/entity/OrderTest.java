package com.sparta.logistics.order.order.entity;

import com.sparta.logistics.order.order.enums.OrderStatus;
import com.sparta.logistics.order.orderitem.entity.OrderItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderTest {

    private static final UUID REQUESTER_COMPANY_ID = UUID.randomUUID();
    private static final UUID RECEIVER_COMPANY_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDateTime DUE_DATE = LocalDateTime.now().plusDays(7);

    // 주문 생성 시 상태는 PENDING, 총금액은 0, 모든 필드가 정상 설정되는지 검증
    @Test
    void create_initializesWithPendingStatusAndZeroTotal() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, "메모");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualTo(0L);
        assertThat(order.getRequesterCompanyId()).isEqualTo(REQUESTER_COMPANY_ID);
        assertThat(order.getReceiverCompanyId()).isEqualTo(RECEIVER_COMPANY_ID);
        assertThat(order.getRequesterUserId()).isEqualTo(USER_ID);
        assertThat(order.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(order.getRequestMemo()).isEqualTo("메모");
        assertThat(order.getOrderItems()).isEmpty();
    }

    // addOrderItem 호출 시 orderItems 리스트에 항목이 추가되는지 검증
    @Test
    void addOrderItem_appendsToList() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, null);
        OrderItem item = mock(OrderItem.class);

        order.addOrderItem(item);

        assertThat(order.getOrderItems()).hasSize(1).contains(item);
    }

    // 여러 OrderItem의 subTotal 합산이 totalAmount에 올바르게 반영되는지 검증
    @Test
    void calculateTotalAmount_sumsAllItemSubtotals() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, null);
        OrderItem item1 = mock(OrderItem.class);
        OrderItem item2 = mock(OrderItem.class);
        when(item1.getSubTotal()).thenReturn(10_000L);
        when(item2.getSubTotal()).thenReturn(5_000L);

        order.addOrderItem(item1);
        order.addOrderItem(item2);
        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualTo(15_000L);
    }

    // 주문 항목이 없을 때 totalAmount가 0인지 검증
    @Test
    void calculateTotalAmount_returnsZeroWithNoItems() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, null);

        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualTo(0L);
    }

    // 값을 전달하면 dueDate와 requestMemo가 정상적으로 변경되는지 검증
    @Test
    void update_changesDueDateAndMemo() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, "원래 메모");
        LocalDateTime newDueDate = DUE_DATE.plusDays(3);

        order.update(newDueDate, "새 메모");

        assertThat(order.getDueDate()).isEqualTo(newDueDate);
        assertThat(order.getRequestMemo()).isEqualTo("새 메모");
    }

    // null을 전달하면 기존 dueDate와 requestMemo가 그대로 유지되는지 검증
    @Test
    void update_retainsPreviousValuesOnNullInput() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, "원래 메모");

        order.update(null, null);

        assertThat(order.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(order.getRequestMemo()).isEqualTo("원래 메모");
    }

    // 취소 처리 시 상태가 CANCELLED로 바뀌고 취소자/취소 사유/취소 시각이 기록되는지 검증
    @Test
    void cancel_setsStatusCancelledWithCancelInfo() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, null);

        order.cancel(USER_ID, "단순 변심");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledBy()).isEqualTo(USER_ID);
        assertThat(order.getCancelReason()).isEqualTo("단순 변심");
        assertThat(order.getCancelledAt()).isNotNull();
    }

    // PENDING 상태의 주문은 수정 가능(true)한지 검증
    @Test
    void isModifiable_returnsTrueWhenPending() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, null);

        assertThat(order.isModifiable()).isTrue();
    }

    // CANCELLED 상태의 주문은 수정 불가(false)한지 검증
    @Test
    void isModifiable_returnsFalseWhenCancelled() {
        Order order = Order.create(REQUESTER_COMPANY_ID, RECEIVER_COMPANY_ID, USER_ID, DUE_DATE, null);
        order.cancel(USER_ID, "취소");

        assertThat(order.isModifiable()).isFalse();
    }
}
