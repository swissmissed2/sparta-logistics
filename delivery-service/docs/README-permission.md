# Delivery Service — 권한 가이드

---

## 1. 역할(Role) 정의
> `허브 배송 담당자`와 `업체 배송 담당자` 분기를  
> 현재는 역할(Role)이 아닌 배송 서비스 내 로직(Service)으로 하고 있음.  
> 하지만 추후 정산 등 다른 API가 필요할 시 역할 혹은 공통 분기점을 정비할 필요가 있어 보임.

| 역할 | 설명 | 추가 헤더 |
|------|------|---------|
| `MASTER` | 시스템 관리자 — 모든 리소스 접근 | 없음 |
| `HUB_MANAGER` | 허브 담당자 — 자기 허브 소속 리소스만 접근 | `X-User-HubId` |
| `DELIVERY_MANAGER` | 배송담당자 — 자신에게 배정된 배송·경로만 접근 | 없음 (userId로 식별) |
| `COMPANY_MANAGER` | 업체 담당자 — 자기 업체의 배송·주문만 조회 | `X-User-CompanyId` |

---

## 2. 권한 매트릭스

### 배송 (DeliveryEntity)

| API | MASTER | HUB_MANAGER | DELIVERY_MANAGER | COMPANY_MANAGER |
|-----|:------:|:-----------:|:----------------:|:---------------:|
| 배송 단건 조회 | ✅ 전체 | ✅ 자기 허브 소속 | ✅ 자신 담당 배송² | ✅ 전체 허용¹ |
| 배송 목록 조회 | ✅ 전체 | ✅ 자기 허브 필터 | ✅ 자신 담당 필터² | ✅ 전체 허용¹ |
| 배송 수정 | ✅ | ✅ 자기 허브 소속 | ❌ | ❌ |
| 배송 상태 변경 | ✅ | ✅ 자기 허브 소속 | ✅ 자신 담당 배송² | ❌ |
| 배송 삭제 | ✅ | ❌ | ❌ | ❌ |

> ¹ COMPANY_MANAGER의 배송 소유 검증(companyId → orderId)은 협의 후 구현 예정.  
> 현재는 COMPANY_MANAGER에게 모든 배송 조회를 허용 (임시 정책).
>
> ² DELIVERY_MANAGER의 배송 접근은 **담당자 타입에 따라 범위가 다릅니다** — 아래 [섹션 5. DELIVERY_MANAGER 타입별 접근 범위](#5-delivery_manager-타입별-접근-범위) 참고.

### 배송담당자 (DeliveryManagerEntity)

| API | MASTER | HUB_MANAGER | DELIVERY_MANAGER | COMPANY_MANAGER |
|-----|:------:|:-----------:|:----------------:|:---------------:|
| 목록 조회 | ✅ 전체 | ✅ 자기 허브만 | ✅ 본인만 | ❌ |
| 단건 조회 | ✅ | ✅ 자기 허브만 | ✅ 본인만 | ❌ |
| 생성 | ✅ | ✅ 자기 허브만 | ❌ | ❌ |
| 수정 | ✅ | ✅ 자기 허브만 | ✅ 본인만 | ❌ |
| 상태 변경 | ✅ | ✅ 자기 허브만 | ✅ 본인만 | ❌ |
| 삭제 | ✅ | ✅ 자기 허브만 | ❌ | ❌ |

### 배송경로 (DeliveryRouteEntity)

| API | MASTER | HUB_MANAGER | DELIVERY_MANAGER | COMPANY_MANAGER |
|-----|:------:|:-----------:|:----------------:|:---------------:|
| 목록 조회 | ✅ | ✅ 자기 허브 배송 | ✅ 자신 담당 배송 | ✅ 자기 업체 배송 |
| 경로 수정 | ✅ | ✅ 자기 허브 배송 | ✅ 자신 담당 구간² | ❌ |

> ² `route.hubDeliveryManagerId == userId`인 구간만 수정 가능.

### 이벤트 로그 (DeliveryLogEntity)

| API | MASTER | HUB_MANAGER | DELIVERY_MANAGER | COMPANY_MANAGER |
|-----|:------:|:-----------:|:----------------:|:---------------:|
| 로그 조회 | ✅ | ✅ 자기 허브 배송 | ✅ 자신 담당 배송 | ✅ 자기 업체 배송 |

> 배송 READ 권한과 동일한 규칙 적용

---

## 3. DeliveryPermissionChecker 구현

`service/DeliveryPermissionChecker.java` — 서비스 레이어 helper component

```java
@Component
public class DeliveryPermissionChecker {

    // 배송 단건 조회 — 위반 시 403
    checkDeliveryReadPermission(delivery, userId, role, hubId, companyId)

    // 배송 수정 — MASTER, HUB_MANAGER만
    checkDeliveryWritePermission(delivery, userId, role, hubId)

    // 배송 상태 변경 — MASTER, HUB_MANAGER, DELIVERY_MANAGER
    checkDeliveryStatusChangePermission(delivery, userId, role, hubId)

    // 배송 삭제 — MASTER만
    checkDeletePermission(role)

    // 배송담당자 조회 — MASTER, HUB_MANAGER, DELIVERY_MANAGER(본인)
    checkManagerReadPermission(manager, userId, role, hubId)

    // 배송담당자 생성/삭제 — MASTER, HUB_MANAGER
    checkManagerWritePermission(targetHubId, userId, role, hubId)

    // 배송담당자 수정/상태변경 — MASTER, HUB_MANAGER, DELIVERY_MANAGER(본인)
    checkManagerSelfWritePermission(manager, userId, role, hubId)

    // 배송경로 수정 — MASTER, HUB_MANAGER, DELIVERY_MANAGER(담당 구간)
    checkRouteWritePermission(delivery, route, userId, role, hubId)
}
```

모든 메서드는 권한 위반 시 `BusinessException(CommonErrorCode.FORBIDDEN)` → HTTP 403 반환.

---

## 4. 자기 허브(Hub) 판단 기준

HUB_MANAGER는 `X-User-HubId`가 배송의 `sourceHubId` 또는 `destinationHubId` 중 하나와 일치하면 접근 허용합니다.

```java
// HUB_MANAGER 판단 로직 (DeliveryPermissionChecker 내부)
if (hubId != null &&
    (hubId.equals(delivery.getSourceHubId()) || hubId.equals(delivery.getDestinationHubId()))) {
    return; // 허용
}
throw new BusinessException(CommonErrorCode.FORBIDDEN);
```

---

## 5. DELIVERY_MANAGER 타입별 접근 범위

`DELIVERY_MANAGER` 역할을 가진 사용자는 `DeliveryManagerEntity`의 `managerType`에 따라 실제 접근 범위가 다릅니다.

| 담당자 타입 | 배송 단건 조회 | 배송 상태 변경 | 배송경로 수정 |
|------------|:------------:|:------------:|:-----------:|
| `COMPANY_DELIVERY` | ✅ `companyDeliveryManagerId == 본인 userId` | ✅ `companyDeliveryManagerId == 본인 userId` | ❌ |
| `HUB_DELIVERY` | ❌ (403) | ❌ (403) | ✅ `hubDeliveryManagerId == 본인 userId` |

**배경**

- `COMPANY_DELIVERY` 담당자는 배송(`p_delivery`) 전체의 마지막 단계를 담당 → `delivery.companyDeliveryManagerId`에 ID가 기록됨
- `HUB_DELIVERY` 담당자는 허브 간 구간(`p_delivery_route`)을 담당 → `route.hubDeliveryManagerId`에 ID가 기록됨

따라서 `HUB_DELIVERY` 타입 담당자는 배송(Delivery)에 직접 ID가 등록되지 않으므로 배송 단건 조회 및 상태 변경에서 403이 반환됩니다. 이 타입의 담당자는 `PUT /deliveries/{id}/routes/{routeId}` 경로 수정 API를 통해서만 작업합니다.

---

## 6. 설계 원칙

| 원칙 | 내용 |
|------|------|
| 권한 검사 위치 | 서비스 레이어 (컨트롤러 X) — DeliveryPermissionChecker 통합 |
| 권한 위반 응답 | **403 Forbidden** (404 아님) — 존재하지만 접근 불가함을 명시 |
| 파라미터 방식 | 개별 파라미터 (`userId`, `role`, `hubId`, `companyId`) — 4개 수준에서 DTO 불필요 |
| COMPANY_MANAGER 예외 | order-service 연동 전까지 임시 전체 허용 |

> 파라미터가 5개 이상으로 늘거나 모든 메서드에서 반복 시 `DeliveryAuthContext` DTO로 리팩토링 예정.
