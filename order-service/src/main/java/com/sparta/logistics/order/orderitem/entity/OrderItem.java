package com.sparta.logistics.order.orderitem.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.order.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "order_id")
    private Order order;

    @Column(nullable = false, name = "product_id")
    private UUID productId;

    @Column(nullable = false, length = 150, name = "product_name")
    private String productName;

    @Column(nullable = false, name = "unit_price")
    private Long unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, name = "sub_total")
    private Long subTotal;

    public static OrderItem create(Order order, UUID productId, String productName, Long unitPrice, Integer quantity) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.productId = productId;
        item.productName = productName;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.subTotal = unitPrice * quantity;
        return item;
    }
}
