package com.sparta.logistics.order.orderitem.entity;

import com.sparta.logistics.order.order.entity.Order;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderItemTest {

    // subTotal이 단가 × 수량으로 올바르게 계산되는지 검증
    @Test
    void create_calculatesSubTotalAsUnitPriceTimesQuantity() {
        Order order = mock(Order.class);
        UUID productId = UUID.randomUUID();

        OrderItem item = OrderItem.create(order, productId, "테스트 상품", 5_000L, 3);

        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getProductName()).isEqualTo("테스트 상품");
        assertThat(item.getUnitPrice()).isEqualTo(5_000L);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getSubTotal()).isEqualTo(15_000L);
    }

    // 수량이 1일 때 subTotal이 단가와 동일한지 검증
    @Test
    void create_subTotalEqualsUnitPriceWhenQuantityIsOne() {
        Order order = mock(Order.class);

        OrderItem item = OrderItem.create(order, UUID.randomUUID(), "상품", 10_000L, 1);

        assertThat(item.getSubTotal()).isEqualTo(10_000L);
    }
}
