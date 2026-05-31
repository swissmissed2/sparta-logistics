# Delivery Service — 엔티티 설계 & DB 스키마

---

## 1. 엔티티 관계도

```
p_delivery (DeliveryEntity)
    │ 1
    │
    │ N
p_delivery_route (DeliveryRouteEntity)   ← @ManyToOne(fetch=LAZY)

p_delivery_log (DeliveryLogEntity)
    └── delivery_id (UUID, 간접 참조 — 외래키 없음, append-only)

p_delivery_manager (DeliveryManagerEntity)
    └── hub_delivery_manager_id → p_delivery_route.hub_delivery_manager_id 에서 참조
    └── company_delivery_manager_id → p_delivery.company_delivery_manager_id 에서 참조
```

---

## 2. `p_delivery` — DeliveryEntity

```sql
CREATE TABLE p_delivery (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                    UUID NOT NULL UNIQUE,        -- 주문 중복 방지 (멱등성)
    status                      VARCHAR(50) NOT NULL,        -- DeliveryStatus ENUM
    source_hub_id               UUID NOT NULL,
    destination_hub_id          UUID NOT NULL,
    current_hub_id              UUID,
    delivery_address            VARCHAR(255) NOT NULL,
    receiver_id                 UUID,
    receiver_slack_id           VARCHAR(100) NOT NULL,
    company_delivery_manager_id UUID,                       -- 업체 배송담당자 ID
    version                     BIGINT NOT NULL DEFAULT 0,  -- 낙관적 락
    final_dispatch_deadline_at  TIMESTAMP,
    started_at                  TIMESTAMP,
    completed_at                TIMESTAMP,
    -- BaseEntity 상속
    created_at                  TIMESTAMP NOT NULL,
    created_by                  UUID,
    updated_at                  TIMESTAMP,
    updated_by                  UUID,
    deleted_at                  TIMESTAMP,
    deleted_by                  UUID
);
```

### 주요 설계 결정

| 항목 | 결정 | 이유                                                             |
|------|------|----------------------------------------------------------------|
| `order_id UNIQUE` | ✅ | Kafka at-least-once 중복 소비 방어 (멱등성 최종 방어선)                      |
| `@Version version` | ✅ | 동시 상태 변경·수정·삭제 Non-Repeatable Read 방지                          |
| `company_delivery_manager_id` | `company_delivery_manager_id`는 배송에만 존재, `hub_delivery_manager_id`는 배송 경로에만 존재 | join 조회가 필요하다는 단점이 있으나, 각각 한 주문에서 배정이 N개와 1개로 나뉘어 구분할 필요성을 느낌. |
| soft delete | `deleted_at + deleted_by` | BaseEntity.softDelete() 활용, 데이터 보존                             |

### DeliveryStatus Enum

```
CREATED → HUB_WAITING → HUB_MOVING → DESTINATION_HUB_ARRIVED → OUT_FOR_DELIVERY → COMPLETED
                                                                                  ↘
모든 상태 → CANCELLED (COMPLETED 제외)
```

---

## 3. `p_delivery_route` — DeliveryRouteEntity

```sql
CREATE TABLE p_delivery_route (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id             UUID NOT NULL REFERENCES p_delivery(id),
    sequence                INT NOT NULL,               -- 0-based 구간 순서
    route_type              VARCHAR(50) NOT NULL,        -- HUB_TO_HUB | HUB_TO_COMPANY ENUM
    source_hub_id           UUID NOT NULL,
    destination_hub_id      UUID,                       -- HUB_TO_COMPANY 구간은 null 가능
    estimated_distance      DECIMAL(10,3) NOT NULL,     -- 예상 거리 (km)
    estimated_duration      INT NOT NULL,               -- 예상 소요 시간 (분)
    actual_distance         DECIMAL(10,3),              -- 실제 거리 (km)
    actual_duration         INT,                        -- 실제 소요 시간 (분)
    status                  VARCHAR(50) NOT NULL DEFAULT 'WAITING',
    hub_delivery_manager_id UUID,                       -- 허브 배송담당자 (구간별 담당)
    started_at              TIMESTAMP,                  -- IN_TRANSIT 전환 시 자동 설정
    arrived_at              TIMESTAMP,                  -- ARRIVED 전환 시 자동 설정
    -- BaseEntity 상속
    created_at              TIMESTAMP NOT NULL,
    created_by              UUID,
    updated_at              TIMESTAMP,
    updated_by              UUID,
    deleted_at              TIMESTAMP,
    deleted_by              UUID
);
```

### RouteType Enum

| 값 | 설명 |
|----|------|
| `HUB_TO_HUB` | 허브 ↔ 허브 구간 (허브 배송담당자 담당) |
| `HUB_TO_COMPANY` | 목적지 허브 → 수령 업체 최종 배송 (업체 배송담당자 담당) |

### RouteStatus 전이

```
WAITING → IN_TRANSIT → ARRIVED
                    ↘ CANCELLED
```

---

## 4. `p_delivery_log` — DeliveryLogEntity

```sql
CREATE TABLE p_delivery_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL,             -- 간접 참조 (외래키 없음 — append-only 특성)
    event_type  VARCHAR(50) NOT NULL,       -- DeliveryEventType ENUM
    status      VARCHAR(50),               -- STATUS_CHANGED 시 변경된 DeliveryStatus
    description VARCHAR(255),
    location    VARCHAR(255),
    actor_id    UUID,                      -- 시스템 자동 처리 시 null
    recorded_at TIMESTAMP NOT NULL
    -- BaseEntity 미상속: soft delete 컬럼이 append-only 테이블에 불필요
);

CREATE INDEX idx_delivery_log_delivery_recorded
    ON p_delivery_log (delivery_id, recorded_at);
```

### 설계 결정

| 항목 | 결정 | 이유                                               |
|------|------|--------------------------------------------------|
| BaseEntity 미상속 | ✅ | 기록 데이터는 수정없이 보존되어야 하므로 append-only 테이블로 결정       |
| `recordedAt` 독립 필드 | ✅ | `baseEntity`를 상속하지 않으므로  `createdAt`과 혼용 방지      |
| 외래키 없음 | ✅ | append-only 로그 테이블의 INSERT 성능 우선                 |
| 인덱스 최소화 | `(delivery_id, recorded_at)` 복합만 | append-only → UPDATE/DELETE 없으므로 인덱스 최소화         |
| traceId 필드 없음 | ✅ | 추후 Zipkin 연동 예정 — Zipkin이 자동 수집, 별도 필드 불필요       |
| 동기 저장 | ✅ | 상태 변경과 동일 `@Transactional` — 실패 시 함께 롤백, 데이터 정합성 보장 |

### DeliveryEventType Enum

| 값                  | 기록 시점                                                     |
|--------------------|-----------------------------------------------------------|
| `CREATED`           | 미구현                                                       |
| `STATUS_CHANGED`   | `DeliveryService.changeStatus()` 호출 시                     |
| `ROUTE_UPDATED`    | `DeliveryRouteService.updateRoute()` — status → ARRIVED 시 |
| `CANCELLED`        | `DeliveryService.deleteDelivery()` 호출 시                   |
| `MANAGER_ASSIGNED` | 배송담당자 배정 시 (PR-D 이후 구현)                                   |
| `EXCEPTION`        | 예외 상황 기록 시 (미구현)                                          |

---

## 5. `p_delivery_manager` — DeliveryManagerEntity

```sql
CREATE TABLE p_delivery_manager (
    id                  UUID PRIMARY KEY,       -- 사용자 ID와 동일 (별도 UUID 생성 없음)
    hub_id              UUID NOT NULL,
    slack_id            VARCHAR(100),
    manager_type        VARCHAR(50) NOT NULL,   -- HUB_DELIVERY | COMPANY_DELIVERY ENUM
    delivery_sequence   INT NOT NULL,           -- 라운드 로빈 순서 (배정 시 +1)
    last_assigned_at    TIMESTAMP,
    status              VARCHAR(50) NOT NULL DEFAULT 'IDLE', -- ENUM
    version             BIGINT NOT NULL DEFAULT 0,  -- 낙관적 락 (동시 배정 충돌 방지)
    -- BaseEntity 상속
    created_at          TIMESTAMP NOT NULL,
    created_by          UUID,
    updated_at          TIMESTAMP,
    updated_by          UUID,
    deleted_at          TIMESTAMP,
    deleted_by          UUID
);
```

### 설계 결정

| 항목 | 결정 | 이유 |
|------|------|------|
| PK = userId | ✅ | 사용자 1명 = 배송담당자 1명 보장, 중복 등록 불가 |
| `@Version version` | ✅ | 동시 라운드 로빈 배정 충돌 방지 |
| `delivery_sequence` | 배정 시 +1 | 라운드 로빈 순서 추적 (min sequence → 다음 배정 대상) |

### DeliveryManagerType Enum

| 값 | 역할 | 저장 위치 |
|----|------|---------|
| `HUB_DELIVERY` | 허브 간 구간 담당 | `p_delivery_route.hub_delivery_manager_id` |
| `COMPANY_DELIVERY` | 최종 업체 배송 담당 | `p_delivery.company_delivery_manager_id` |

### DeliveryManagerStatus Enum

| 값 | 설명 |
|----|------|
| `IDLE` | 대기 중 (배정 가능) |
| `WORKING` | 배송 중 (삭제 불가) |
| `INACTIVE` | 비활성 (배정 제외) |
| `WITHDRAWN` | 탈퇴 (soft delete 시 자동 설정) |

### 주의할 포인트
1. 배송 기사 전체 조회 시 join 필요
2. 배송 상태 전이: 배송기사가 '배송'에서 '루트' 혹은 그 반대로 교체되는 시점에  
데이터 정합성 문제가 생기지 않도록 상태 변경 이벤트(Event) 관리 로직 수정 필요
3. 차후 sequence가 무한히 커지는 문제에 대한 대책 필요
4. 주문과 배송이 1대1이 아닌 1:N개가 되었으므로 배정 로직도 이에 맞게 수정 필요 (260525 결정사항에 따름)

---

## 6. BaseEntity (Common)

```java
// 해당 코드는 실제 코드가 아닌, Type 및 주요 Method만 확인하는 용도입니다.
// common/src/main/java/com/sparta/logistics/common/domain/BaseEntity.java
@MappedSuperclass
public abstract class BaseEntity {
    @CreatedDate   LocalDateTime createdAt;
    @CreatedBy     UUID createdBy;
    @LastModifiedDate LocalDateTime updatedAt;
    @LastModifiedBy UUID updatedBy;
    LocalDateTime deletedAt;
    UUID deletedBy;

    public void softDelete(UUID deleted_by) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deleted_by;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
```

`DeliveryEntity`, `DeliveryRouteEntity`, `DeliveryManagerEntity`는 BaseEntity를 상속합니다.  
`DeliveryLogEntity`는 append-only 특성으로 BaseEntity를 **상속하지 않습니다**.
