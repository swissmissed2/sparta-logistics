# 배송 서비스 Phase 2 변경 사항

---

## 1. 이벤트 로그 저장 전략

**결정: 동기 저장 (같은 @Transactional)**

- 로그 INSERT는 상태 변경과 동일한 `@Transactional` 범위 안에서 실행됨
- 상태 변경 실패 시 로그도 함께 롤백됨 (데이터 정합성 보장)
- 로그 테이블(`p_delivery_log`)은 append-only이므로 인덱스는 `(delivery_id, recorded_at)` 복합 인덱스만 생성
- 팀 Zipkin 연동: 분산 추적은 Zipkin이 자동 수집 → 로그 엔티티에 traceId 필드 추가 불필요
- 성능 임팩트: 일 수십만 건 규모까지 추가 지연 1~5ms 수준으로 허용 가능

```java
// PR-C, DeliveryService.changeStatus() 내부 (같은 트랜잭션)
entity.changeStatus(next);
deliveryLogRepository.save(new DeliveryLogEntity(deliveryId, STATUS_CHANGED, next, null, null, userId));
```

---

## 2. DeliveryLogEntity — `recordedAt` 필드명 유지

**변경하지 않은 이유**

| 선택지 | 결과 |
|--------|------|
| `BaseEntity` 상속 + `createdAt` 재사용 | soft delete 컬럼(`deletedAt`, `deletedBy`)이 append-only 테이블에 불필요하게 추가됨 |
| `recordedAt` 독립 필드 유지 (채택) | 테이블 스펙(`recorded_at`) 일치, append-only 특성 명확 |

---

## 3. DeliveryPermissionChecker — 개별 파라미터 방식 채택

**결정: 개별 파라미터 (`userId`, `role`, `hubId`, `companyId`)**

- DTO(`DeliveryAuthContext`) 도입 시 추가 클래스·빌더 조립 비용 대비 이득 없음 (현재 4개 파라미터 수준)
- 파일 위치: `service/DeliveryPermissionChecker.java`

```java
// 사용 예시 (DeliveryService 내)
permissionChecker.checkDeliveryReadPermission(entity, userId, role, hubId, companyId);
permissionChecker.checkDeliveryWritePermission(entity, userId, role, hubId);
permissionChecker.checkDeletePermission(role); // MASTER만 허용
```

> 구현 중 파라미터가 5개 이상으로 늘거나 모든 메서드에서 반복이 심하면 DTO로 리팩토링.

---

## 4. 배송담당자 삭제 검증 — PR별 단계적 구현

**PR-B (현재)**: `manager.status == WORKING` 체크만 수행

```java
if (manager.getStatus() == DeliveryManagerStatus.WORKING) {
    throw new BusinessException(DeliveryErrorCode.MANAGER_IN_DELIVERY);
}
manager.delete(actorId); // softDelete + status → WITHDRAWN
```

| 방식 | 장점 | 단점 |
|------|------|------|
| `manager.status` 체크 (PR-B 채택) | 추가 쿼리 없음, 빠름 | 상태 동기화 실패 시 오검증 가능 |
| `DeliveryRoute` IN_TRANSIT 조회 | 실제 상태 기준으로 정확 | PR-D 이후에야 가능, 추가 쿼리 필요 |

> PR-D 완료 후 `DeliveryRoute.status == IN_TRANSIT` 구간 조회로 추가 강화 가능.

---

## 5. PR 경계 명시

| PR | 담당 범위 | 명시적 배제 항목 |
|----|----------|----------------|
| **PR-A** | 엔티티·Enum·Repository·DeliveryPermissionChecker | — |
| **PR-B** | 배송담당자 CRUD (`DeliveryManagerEntity` 범위) | DeliveryRoute 연동, 진행 중 배송경로 정합성 체크 |
| **PR-C** | 배송 수정·상태변경·삭제 (`DeliveryEntity` 범위) | 배송경로 상태와의 연동 |
| **PR-D** | 경로·로그 + PR-B/C 미완료 강화 로직 | — |

---

## 6. `deliveryManagerId` → `companyDeliveryManagerId` 필드명 변경

**변경 이유**
(SA문서에 따름)

테이블 스펙에는 두 종류의 배송 담당자가 존재:
- 허브 배송 담당자(HUB_DELIVERY): 구간별 담당 → `p_delivery_route.hub_delivery_manager_id`에 저장
- 업체 배송 담당자(COMPANY_DELIVERY): 배송 전체의 마지막 단계 → `p_delivery`에 저장

`p_delivery.delivery_manager_id`가 실질적으로 "업체 배송 담당자 ID"를 의미하므로 명칭을 명확히 함.

**변경 전 → 후**

| 파일 | 변경 전 | 변경 후 |
|------|--------|--------|
| `entity/DeliveryEntity.java` | `deliveryManagerId` | `companyDeliveryManagerId` |
| `dto/DeliveryDetailResponse.java` | `UUID deliveryManagerId` | `UUID companyDeliveryManagerId` |
| `dto/DeliverySearchCond.java` | `UUID deliveryManagerId` | `UUID companyDeliveryManagerId` |
| `repository/DeliveryRepository.java` | JPQL `d.deliveryManagerId` | `d.companyDeliveryManagerId` |
| DB 컬럼명 | `delivery_manager_id` | `company_delivery_manager_id` |

> `ddl-auto: create-drop` 사용 중이므로 Flyway 마이그레이션 스크립트 불필요.

---

## 7. 동시성 분석 및 수정 내역

### 전제: PostgreSQL 기본 격리 수준 = READ COMMITTED

| 문제 | 위험도 | 발생 구간 | 현재 상태 |
|------|--------|----------|----------|
| Dirty Read | ❌ 없음 | — | DB 자동 차단 |
| Non-Repeatable Read | 🔴 즉시 수정 | `DeliveryService.changeStatus()` | `@Version` 추가로 해결 |
| Phantom Read (sequence) | 🟡 기능적 허용 | `createManager().findMaxDeliverySequence()` | 라운드로빈 동작은 정상, 장기 개선 예정 |
| Phantom Read (assignment) | 🟡 처리됨 | `findNextAssignee()` 라운드 로빈 | `@Version` 충돌 → 409 반환 |
| Deadlock | 🟢 현재 안전 | — | 낙관적 락 사용 중 |

---

### 7-1. Fix: `DeliveryEntity`에 `@Version` 추가

**문제 시나리오 (수정 전)**
```
T1: SELECT delivery → status=CREATED → canTransitionTo(HUB_WAITING)=true
T2: SELECT delivery → status=CREATED → canTransitionTo(HUB_WAITING)=true  ← T1 미커밋
T1: UPDATE status=HUB_WAITING → commit
T2: UPDATE status=HUB_WAITING → commit  ← 중복 상태 전이 성공 (버그)
```

**수정 후**
```java
// entity/DeliveryEntity.java
@Version
private long version;
// T2가 commit 시도 → version 불일치 → ObjectOptimisticLockingFailureException
```

영향 범위: `changeStatus()`, `updateDelivery()`, `deleteDelivery()` 모두 보호됨.

---

### 7-2. Fix: `GlobalExceptionHandler`에 409 핸들러 추가
> `DeliveryExceptionHandler` 로 임시 이동  
> 차후 협의 후 Common에 추가 고려

```java
// common/.../GlobalExceptionHandler.java
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(...) {
    return ResponseEntity.status(409).body(ApiResponse.error(CommonErrorCode.CONFLICT));
}
```

클라이언트가 409를 받으면 잠시 후 재시도하도록 유도.

---

### 7-3. 알려진 한계 (장기 개선 항목)

| 항목 | 내용 | 개선 시점 |
|------|------|---------|
| `findMaxDeliverySequence()` Phantom Read | 동시 담당자 등록 시 초기 sequence 중복 가능. 라운드 로빈 동작에는 영향 없음 | Flyway 도입 시 DB SEQUENCE(`nextval`) 전환 |
| `findNextAssignee()` 재시도 없음 | 멀티 파티션 환경에서 배송 생성 409 가능 | Kafka 파티션 전략 확정 후 `@Retryable` 추가 |
| 페이지네이션 Phantom Read | offset 기반 특성 — 페이지 이동 중 데이터 삽입 시 중복/누락 가능 | 커서 기반 페이지네이션으로 전환 시 해결 |

---

## 8. 멱등성 분석 및 수정 내역

> **멱등성(Idempotency)**: 동일한 요청을 여러 번 호출해도 첫 번째와 동일한 결과를 보장하는 성질.  
> Kafka at-least-once 특성과 HTTP 클라이언트 재시도 시나리오를 기준으로 전체 API·이벤트 핸들러를 점검.

### 현황 요약

| API / 메서드 | 중복 호출 결과 | 위험도 |
|---|---|---|
| `handleStockReserved()` + `createDelivery()` | `DataIntegrityViolationException` → 500 → Kafka 재시도 무한 루프 | 🔴 수정 완료 |
| `createManager()` | PK(userId) 중복 → 500 | 🔴 수정 완료 |
| `GlobalExceptionHandler` | `DataIntegrityViolationException` 미처리 → 500 | 🔴 수정 완료 |
| `updateRoute(status=ARRIVED)` 반복 | 중복 로그 INSERT | 🟡 수정 완료 |
| `handleAiDeadlineCalculated()` | 동일 값 덮어쓰기 | ✅ 이미 멱등 |
| `updateDelivery()` / `updateManager()` / `updateRoute()` PUT | 동일 payload → 동일 결과 | ✅ 이미 멱등 |
| `changeManagerStatus()` | 동일 상태 재설정 — 오류 없음 | ✅ 이미 멱등 |
| `deleteDelivery()` / `deleteManager()` 2차 | 400 ALREADY_DELETED | ✅ 허용 가능 (REST 표준) |
| `changeStatus()` 동일 상태 재요청 | 400 INVALID_STATUS_TRANSITION | ✅ 허용 가능 (상태 전이 특성) |

---

### Fix 1: `createDelivery()` — orderId 중복 방어 (Kafka 멱등성)

**문제**: Kafka at-least-once 특성으로 `stock.reserved` 메시지 2회 소비 시, `orderId` UNIQUE 제약 위반 → `DataIntegrityViolationException` → 500 → Kafka가 재시도 → 무한 루프.

**수정 파일**:
- `repository/DeliveryRepository.java` — `boolean existsByOrderId(UUID orderId)` 추가
- `service/DeliveryService.java` — `createDelivery()` 상단에 존재 체크 + 조기 반환

```java
// DeliveryService.createDelivery()
if (deliveryRepository.existsByOrderId(event.orderId())) {
    log.info("[createDelivery] 이미 처리된 주문 — orderId={}", event.orderId());
    return;
}
```

> `existsByOrderId` + `save` 사이의 TOCTOU 경쟁은 Fix 3 (safety-net 핸들러)으로 최종 봉쇄.

---

### Fix 2: `createManager()` — userId(PK) 중복 방어

**문제**: 동일 `userId`로 POST 2회 호출 시 `@Id` PK 중복 → `DataIntegrityViolationException` → 500.

**수정 파일**:
- `exception/DeliveryErrorCode.java` — `MANAGER_ALREADY_EXISTS(CONFLICT, "DELIVERY_MGR_409", ...)` 추가
- `service/DeliveryManagerService.java` — `createManager()` 상단에 `existsById()` 체크 후 409 반환

```java
// DeliveryManagerService.createManager()
if (managerRepository.existsById(req.userId())) {
    throw new BusinessException(DeliveryErrorCode.MANAGER_ALREADY_EXISTS);
}
```

---

### Fix 3: `GlobalExceptionHandler` — `DataIntegrityViolationException` safety-net

**문제**: Fix 1·2의 애플리케이션 레벨 중복 체크를 동시 요청이 통과하면 DB 제약 위반이 500으로 노출됨.

**수정 파일**: `common/.../GlobalExceptionHandler.java`

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(...) {
    // → 409 Conflict 반환 (500 노출 차단)
}
```

---

### Fix 4: `updateRoute()` — 동일 상태 재요청 시 로그 중복 INSERT 방지

**문제**: `PUT /routes/{id}` with `status=ARRIVED` 2회 호출 시, `p_delivery_log`에 동일 구간 도착 이벤트 2건 삽입.

**수정 파일**: `service/DeliveryRouteService.java`

```java
// 멱등성 보장: 동일 상태 재요청 시 changeStatus 및 로그 INSERT 생략
if (req.status() != null && route.getStatus() != req.status()) {
    route.changeStatus(req.status());
    if (req.status() == RouteStatus.ARRIVED) { logRepository.save(...); }
}
```
