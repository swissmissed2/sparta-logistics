package com.sparta.logistics.delivery.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.delivery.dto.DeliveryUpdateRequest;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryEntity extends BaseEntity {
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
    private UUID companyDeliveryManagerId;

    @Version
    private long version; // 낙관적 락 — 동시 상태 변경·수정·삭제 충돌 방지

    @Column
    private LocalDateTime finalDispatchDeadlineAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    public DeliveryEntity(UUID orderId, UUID receiverId,
                          UUID sourceHubId, UUID destinationHubId,
                          String deliveryAddress, String receiverSlackId) {
        this.orderId = orderId;
        this.receiverId = receiverId;
        this.sourceHubId = sourceHubId;
        this.destinationHubId = destinationHubId;
        this.deliveryAddress = deliveryAddress;
        this.receiverSlackId = receiverSlackId;
        this.status = DeliveryStatus.CREATED;
    }

    public void updateReceiverInfo(String deliveryAddress, String receiverSlackId) {
        this.deliveryAddress = deliveryAddress;
        this.receiverSlackId = receiverSlackId;
    }

    public void update(DeliveryUpdateRequest req) {
        if (req.deliveryAddress() != null) this.deliveryAddress = req.deliveryAddress();
        if (req.receiverSlackId() != null) this.receiverSlackId = req.receiverSlackId();
        if (req.currentHubId() != null) this.currentHubId = req.currentHubId();
        if (req.companyDeliveryManagerId() != null) this.companyDeliveryManagerId = req.companyDeliveryManagerId();
    }

    public void changeStatus(DeliveryStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new BusinessException(DeliveryErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = next;
        if (next == DeliveryStatus.OUT_FOR_DELIVERY) {
            this.startedAt = LocalDateTime.now();
        } else if (next == DeliveryStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void updateFinalDispatchDeadline(LocalDateTime deadline) {
        this.finalDispatchDeadlineAt = deadline;
    }

    public void assignCompanyDeliveryManager(UUID managerId) {
        this.companyDeliveryManagerId = managerId;
    }

    public void updateCurrentHub(UUID hubId) {
        this.currentHubId = hubId;
    }

    public boolean canCancel() {
        return this.status.canTransitionTo(DeliveryStatus.CANCELLED);
    }

    public void delete(UUID actorId) {
        softDelete(actorId);
    }
}
