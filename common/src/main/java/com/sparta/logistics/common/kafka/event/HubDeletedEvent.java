package com.sparta.logistics.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 토픽: hub.deleted
 * 발행: HubService / 구독: DeliveryService
 * <p>
 * 허브 소프트딜리트 시 발행 — 해당 허브 소속 배송담당자 논리 삭제 연동
 * 파티션 키: hubId
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubDeletedEvent {
    private UUID eventId;
    private UUID hubId;
    private UUID deletedBy;
}
