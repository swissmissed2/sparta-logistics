package com.sparta.logistics.order.order.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.order.order.enums.OrderStatus;
import com.sparta.logistics.order.orderitem.entity.OrderItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "requester_company_id")
    private UUID requesterCompanyId;

    @Column(nullable = false, name = "receiver_company_id")
    private UUID receiverCompanyId;

    @Column(nullable = false, name = "requester_user_id")
    private UUID requesterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, name = "total_amount")
    private Long totalAmount;

    @Column(nullable = false, name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "request_memo")
    private String requestMemo;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public static Order create(UUID requesterCompanyId, UUID receiverCompanyId, UUID requesterUserId, LocalDateTime dueDate, String requestMemo) {
        Order order = new Order();
        order.requesterCompanyId = requesterCompanyId;
        order.receiverCompanyId = receiverCompanyId;
        order.requesterUserId = requesterUserId;
        order.status = OrderStatus.PENDING;
        order.totalAmount = 0L;
        order.dueDate = dueDate;
        order.requestMemo = requestMemo;
        return order;
    }

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
    }

    public void calculateTotalAmount() {
        this.totalAmount = orderItems.stream()
                .mapToLong(OrderItem::getSubTotal)
                .sum();
    }

    public void update(LocalDateTime dueDate, String requestMemo) {
        if (dueDate != null) this.dueDate = dueDate;
        if (requestMemo != null) this.requestMemo = requestMemo;
    }

    public void cancel(UUID cancelledBy, String cancelReason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
    }

    public boolean isModifiable() {
        return this.status != OrderStatus.CANCELLED
                && this.status != OrderStatus.COMPLETED
                && this.status != OrderStatus.IN_DELIVERY;
    }
}
