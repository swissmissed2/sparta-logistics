package com.sparta.logistics.delivery.dto;

import com.sparta.logistics.delivery.entity.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public record DeliveryCreateRequest {
    // TODO: Entity 다 가져오기
    // @ 어노테이션 붙이기
    // 필요없는 col 제거하기
    @NotNull(message = "업체 타입은 필수입니다.")
    CompanyType type,


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
}