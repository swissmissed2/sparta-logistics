package com.sparta.logistics.delivery.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.delivery.entity.enums.RouteStatus;
import com.sparta.logistics.delivery.entity.enums.RouteType;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_delivery_route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRouteEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryEntity delivery;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteType routeType;

    @Column(nullable = false)
    private UUID sourceHubId;

    @Column
    private UUID destinationHubId; // HUB_TO_COMPANY 구간은 null 가능

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal estimatedDistance;

    @Column(nullable = false)
    private int estimatedDuration; // 예상 소요 시간 (분)

    @Column(precision = 10, scale = 3)
    private BigDecimal actualDistance;

    @Column
    private Integer actualDuration; // 실제 소요 시간 (분)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteStatus status = RouteStatus.WAITING;

    @Column
    private UUID hubDeliveryManagerId;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime arrivedAt;

    @Version
    @SuppressWarnings("unused")
    private long version; // 낙관적 락 — 동시 route 상태 변경 충돌 방지

    public DeliveryRouteEntity(DeliveryEntity delivery, int sequence, RouteType routeType,
                               UUID sourceHubId, UUID destinationHubId,
                               BigDecimal estimatedDistance, int estimatedDuration) {
        this.delivery = delivery;
        this.sequence = sequence;
        this.routeType = routeType;
        this.sourceHubId = sourceHubId;
        this.destinationHubId = destinationHubId;
        this.estimatedDistance = estimatedDistance;
        this.estimatedDuration = estimatedDuration;
        this.status = RouteStatus.WAITING;
    }

    public void updateActual(BigDecimal actualDistance, Integer actualDuration) {
        this.actualDistance = actualDistance;
        this.actualDuration = actualDuration;
    }

    public void changeStatus(RouteStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new BusinessException(DeliveryErrorCode.INVALID_ROUTE_STATUS_TRANSITION);
        }
        this.status = next;
        if (next == RouteStatus.IN_TRANSIT) {
            this.startedAt = LocalDateTime.now();
        } else if (next == RouteStatus.ARRIVED) {
            this.arrivedAt = LocalDateTime.now();
        }
    }

    public void assignManager(UUID managerId) {
        this.hubDeliveryManagerId = managerId;
    }
}
