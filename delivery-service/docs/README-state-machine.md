# Delivery Service — 배송 상태 전이 가이드

---

## 1. 배송 상태 전이 다이어그램

```
                     ┌─────────────────────────────────────────┐
                     │                CANCELLED                │
                     │  (모든 상태에서 CANCELLED 가능,            │
                     │   COMPLETED 제외)                        │
                     └─────────────────────────────────────────┘
                              ↑         ↑         ↑        ↑
                              │         │         │        │
   [Kafka stock.reserved]     │         │         │        │
         ↓                    │         │         │        │
    ┌─────────┐      ┌────────────┐  ┌───────────┐  ┌──────────────────────┐
    │ CREATED │─────▶│HUB_WAITING│─▶│ HUB_MOVING│─▶│ DESTINATION_HUB_    │
    └─────────┘      └────────────┘  └───────────┘  │     ARRIVED         │
                                                     └─────────────────────┘
                                                                │
                                                                ▼
                                                      ┌─────────────────┐
                                                      │ OUT_FOR_DELIVERY│ ──▶ ┌───────────┐
                                                      └─────────────────┘     │ COMPLETED │
                                                                               └───────────┘
                                                                                  (종료 상태)
```

---

## 2. 허용 전이 목록

| 현재 상태 | 허용되는 다음 상태 |
|----------|-----------------|
| `CREATED` | `HUB_WAITING`, `CANCELLED` |
| `HUB_WAITING` | `HUB_MOVING`, `CANCELLED` |
| `HUB_MOVING` | `DESTINATION_HUB_ARRIVED`, `CANCELLED` |
| `DESTINATION_HUB_ARRIVED` | `OUT_FOR_DELIVERY`, `CANCELLED` |
| `OUT_FOR_DELIVERY` | `COMPLETED`, `CANCELLED` |
| `COMPLETED` | _(없음 — 종료 상태)_ |
| `CANCELLED` | _(없음 — 종료 상태)_ |

---

## 3. 각 상태 의미

| 상태 | 의미 | 자동 설정 필드                            |
|------|------|-------------------------------------|
| `CREATED` | Kafka `stock.reserved` 소비 후 배송 생성됨 | 현재 구현 중                             |
| `HUB_WAITING` | 출발 허브에서 허브 간 이동 대기 중 | —                                   |
| `HUB_MOVING` | 허브 간 이동 중 | —                                   |
| `DESTINATION_HUB_ARRIVED` | 목적지 허브 도착 | —                                   |
| `OUT_FOR_DELIVERY` | 목적지 허브 → 수령 업체로 최종 배송 출발 | `startedAt = LocalDateTime.now()`   |
| `COMPLETED` | 배송 완료 | `completedAt = LocalDateTime.now()` |
| `CANCELLED` | 취소됨 | —                                   |

---

## 4. 구현: `DeliveryStatus.canTransitionTo()`

```java
// entity/DeliveryStatus.java
private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED = Map.of(
    CREATED,                 Set.of(HUB_WAITING, CANCELLED),
    HUB_WAITING,             Set.of(HUB_MOVING, CANCELLED),
    HUB_MOVING,              Set.of(DESTINATION_HUB_ARRIVED, CANCELLED),
    DESTINATION_HUB_ARRIVED, Set.of(OUT_FOR_DELIVERY, CANCELLED),
    OUT_FOR_DELIVERY,        Set.of(COMPLETED, CANCELLED),
    COMPLETED,               Set.of(),
    CANCELLED,               Set.of()
);

public boolean canTransitionTo(DeliveryStatus next) {
    return ALLOWED.getOrDefault(this, Set.of()).contains(next);
}
```

---

## 5. 상태 변경 API 흐름

```
PATCH /api/v1/deliveries/{deliveryId}/status
Body: { "status": "HUB_WAITING" }

1. findActiveOrThrow(deliveryId)         → 404 / 400 (deleted)
2. checkDeliveryStatusChangePermission() → 403
3. entity.getStatus().canTransitionTo()  → false: 400 INVALID_STATUS_TRANSITION
4. entity.changeStatus(next)             → startedAt/completedAt 자동 설정
5. deliveryLogRepository.save(로그)      → 동일 트랜잭션 (STATUS_CHANGED 이벤트)
6. return DeliveryDetailResponse
```

---

## 6. 배송경로 상태 전이

```
WAITING → IN_TRANSIT → ARRIVED
                    ↘ CANCELLED
```

| 상태 | 의미 | 자동 설정 필드 |
|------|------|-------------|
| `WAITING` | 이동 대기 | — |
| `IN_TRANSIT` | 이동 중 | `startedAt = LocalDateTime.now()` |
| `ARRIVED` | 도착 완료 | `arrivedAt = LocalDateTime.now()` + 이벤트 로그 자동 기록 |
| `CANCELLED` | 취소됨 | — |

---

## 7. 상태 전이 관련 동시성 보호

`DeliveryEntity`에 `@Version`이 적용되어 있습니다.

```
T1: SELECT delivery (status=CREATED) → canTransitionTo(HUB_WAITING)=true
T2: SELECT delivery (status=CREATED) → canTransitionTo(HUB_WAITING)=true  (T1 미커밋)
T1: UPDATE status=HUB_WAITING, version=0→1 → commit
T2: UPDATE status=HUB_WAITING, version=0   → ObjectOptimisticLockingFailureException
                                            → 409 Conflict 반환
```

클라이언트는 409 수신 시 잠시 후 재조회 후 재시도합니다.
