package com.sparta.logistics.delivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false)
    private UUID sourceHubId;

    @Column(nullable = false)
    private UUID destinationHubId;

    @Column
    private UUID currentHubId;

    @Column(nullable = false)
    private String deliveryAddress;

    @Column
    private UUID receiverId;

    @Column(nullable = false, length = 100)
    private String receiverSlackId;

    @Column
    private UUID deliveryManagerId;

    @Column
    private LocalDateTime finalDispatchDeadlineAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    // 생성
    public DeliveryEntity(UUID orderId, String deliveryAddress, String receiverSlackId) {
        this.orderId = orderId;
        this.deliveryAddress = deliveryAddress;
        this.receiverSlackId = receiverSlackId;
        this.status = DeliveryStatus.CREATED;
    }

    // 수정 (주소, 슬랙 ID)
    public void updateReceiverInfo(String deliveryAddress, String receiverSlackId) {
        this.deliveryAddress = deliveryAddress;
        this.receiverSlackId = receiverSlackId;
    }

    // 상태 변경
    public void updateStatus(DeliveryStatus status) {
        this.status = status;
    }
}
