# Delivery Service — 전체 API 스펙

> Base URL: `http://localhost:8080` (API Gateway 경유)  
> 모든 요청에 `X-User-Id`, `X-User-Role` 헤더 필수.

---

## 공통 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| `X-User-Id` | ✅ | 요청자 UUID |
| `X-User-Role` | ✅ | `MASTER` \| `HUB_MANAGER` \| `DELIVERY_MANAGER` \| `COMPANY_MANAGER` |
| `X-User-HubId` | 조건부 | HUB_MANAGER 역할 시 필요 |
| `X-User-CompanyId` | 조건부 | COMPANY_MANAGER 역할 시 필요 |

---

## 1. 배송 API (`/api/v1/deliveries`)

### 1-1. 배송 단건 조회

```
GET /api/v1/deliveries/{deliveryId}
```

**응답 200 OK**
```json
{
  "deliveryId": "UUID",
  "orderId": "UUID",
  "status": "CREATED",
  "sourceHubId": "UUID",
  "destinationHubId": "UUID",
  "currentHubId": "UUID | null",
  "deliveryAddress": "서울시 강남구 테헤란로 100",
  "receiverId": "UUID",
  "receiverSlackId": "U12345678",
  "companyDeliveryManagerId": "UUID | null",
  "finalDispatchDeadlineAt": "2025-05-25T14:00:00",
  "startedAt": "2025-05-24T09:00:00 | null",
  "completedAt": "2025-05-24T18:00:00 | null"
}
```

**에러**
| 코드 | HTTP | 설명 |
|------|------|------|
| `DELIVERY_404` | 404 | 배송 없음 |
| `DELIVERY_001` | 400 | 이미 삭제된 배송 |
| `COMMON_403` | 403 | 권한 없음 |

---

### 1-2. 배송 목록 조회

```
GET /api/v1/deliveries?orderId=&status=&sourceHubId=&destinationHubId=&companyDeliveryManagerId=
```

**쿼리 파라미터** (모두 선택)

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `orderId` | UUID | 주문 ID 필터 |
| `status` | DeliveryStatus | 배송 상태 필터 |
| `sourceHubId` | UUID | 출발 허브 필터 |
| `destinationHubId` | UUID | 도착 허브 필터 |
| `companyDeliveryManagerId` | UUID | 업체 배송담당자 필터 |

**페이지네이션** (Pageable)
- `page=0&size=10&sort=createdAt,DESC` (기본값)

**응답 200 OK** — `Page<DeliveryListResponse>`

> 역할별 자동 필터: HUB_MANAGER → 자기 허브 소속만, DELIVERY_MANAGER → 자신 담당만

---

### 1-3. 배송 수정

```
PUT /api/v1/deliveries/{deliveryId}
Content-Type: application/json
```

**요청 Body** (변경할 필드만 포함, null 무시)
```json
{
  "deliveryAddress": "서울시 서초구 반포대로 58",
  "receiverSlackId": "U99999999",
  "currentHubId": "UUID",
  "companyDeliveryManagerId": "UUID"
}
```

**응답 200 OK** — DeliveryDetailResponse

**권한**: MASTER, HUB_MANAGER(자기 허브)  
**에러**: `COMMON_403` (권한 없음), `COMMON_409` (낙관적 락 충돌)

---

### 1-4. 배송 상태 변경

```
PATCH /api/v1/deliveries/{deliveryId}/status
Content-Type: application/json
```

**요청 Body**
```json
{
  "status": "HUB_WAITING"
}
```

**허용 상태 전이** → [`README-state-machine.md`](README-state-machine.md) 참고

**응답 200 OK** — DeliveryDetailResponse

**권한**: MASTER, HUB_MANAGER(자기 허브), DELIVERY_MANAGER(자신 담당)  
**에러**: `DELIVERY_002` (허용되지 않는 전이), `COMMON_409` (낙관적 락 충돌)

---

### 1-5. 배송 삭제 (Soft Delete)

```
DELETE /api/v1/deliveries/{deliveryId}
```

**응답**: 204 No Content

**권한**: MASTER만  
**에러**: `COMMON_403` (권한 없음), `DELIVERY_001` (이미 삭제됨)

---

## 2. [ 수정 요망 ] 배송담당자 API (`/api/v1/delivery-managers`)
> 업체 배송 담당자와 허브 배송 담당자 join 조회로 수정 필요

### 2-1. 배송담당자 생성

```
POST /api/v1/delivery-managers
Content-Type: application/json
```

**요청 Body**
```json
{
  "userId": "UUID",
  "hubId": "UUID",
  "slackId": "U12345678",
  "managerType": "HUB_DELIVERY"
}
```

| 필드 | 설명 |
|------|------|
| `managerType` | `HUB_DELIVERY` (허브 간 구간 담당) \| `COMPANY_DELIVERY` (최종 업체 배송 담당) |

**응답**: 201 Created, Location: `/api/v1/delivery-managers/{managerId}`

**권한**: MASTER, HUB_MANAGER(자기 허브)  
**에러**: `DELIVERY_MGR_409` (이미 등록된 담당자)

---

### 2-2. 배송담당자 목록 조회

```
GET /api/v1/delivery-managers?page=0&size=10&sort=createdAt,DESC
```

**응답 200 OK** — `Page<DeliveryManagerResponse>`

> 역할별 자동 필터: HUB_MANAGER → 자기 허브만, DELIVERY_MANAGER → 본인만

---

### 2-3. 배송담당자 단건 조회

```
GET /api/v1/delivery-managers/{managerId}
```

**응답 200 OK**
```json
{
  "managerId": "UUID",
  "hubId": "UUID",
  "slackId": "U12345678",
  "managerType": "HUB_DELIVERY",
  "deliverySequence": 3,
  "lastAssignedAt": "2025-05-24T10:00:00",
  "status": "IDLE"
}
```

---

### 2-4. 배송담당자 수정

```
PUT /api/v1/delivery-managers/{managerId}
Content-Type: application/json
```

**요청 Body** (변경할 필드만)
```json
{
  "hubId": "UUID",
  "slackId": "U99999999"
}
```

**권한**: MASTER, HUB_MANAGER(자기 허브), DELIVERY_MANAGER(본인)

---

### 2-5. 배송담당자 상태 변경

```
PATCH /api/v1/delivery-managers/{managerId}/status
Content-Type: application/json
```

**요청 Body**
```json
{
  "status": "IDLE"
}
```

| 상태 | 설명 |
|------|------|
| `IDLE` | 대기 중 |
| `WORKING` | 배송 중 |
| `INACTIVE` | 비활성 |
| `WITHDRAWN` | 탈퇴 (삭제 시 자동 설정) |

---

### 2-6. 배송담당자 삭제 (Soft Delete)

```
DELETE /api/v1/delivery-managers/{managerId}
```

**응답**: 204 No Content

**권한**: MASTER, HUB_MANAGER(자기 허브)  
**제약**: status == WORKING인 경우 삭제 불가 (`DELIVERY_MGR_002`)

---

## 3. [ 수정 요망 ] 배송경로 API (`/api/v1/deliveries/{deliveryId}/routes`)
> 다중 배송 건이 하나의 주문으로 합쳐지므로 경로도 여러 개가 필요함. 수정 필요.

### 3-1. 배송경로 목록 조회

```
GET /api/v1/deliveries/{deliveryId}/routes
```

**응답 200 OK**
```json
[
  {
    "routeId": "UUID",
    "sequence": 0,
    "routeType": "HUB_TO_HUB",
    "sourceHubId": "UUID",
    "destinationHubId": "UUID",
    "estimatedDistance": 123.456,
    "estimatedDuration": 90,
    "actualDistance": 125.100,
    "actualDuration": 95,
    "status": "ARRIVED",
    "hubDeliveryManagerId": "UUID",
    "startedAt": "2025-05-24T09:00:00",
    "arrivedAt": "2025-05-24T10:35:00"
  }
]
```

---

### 3-2. 배송경로 수정

```
PUT /api/v1/deliveries/{deliveryId}/routes/{routeId}
Content-Type: application/json
```

**요청 Body** (변경할 필드만)
```json
{
  "actualDistance": 125.100,
  "actualDuration": 95,
  "status": "ARRIVED"
}
```

**status = ARRIVED 시**: 이벤트 로그 자동 기록 (ROUTE_UPDATED)

**권한**: MASTER, HUB_MANAGER(자기 허브), DELIVERY_MANAGER(자신 담당 구간)

---

## 4. 배송 이벤트 로그 API (`/api/v1/deliveries/{deliveryId}/logs`)

### 4-1. 이벤트 로그 조회

```
GET /api/v1/deliveries/{deliveryId}/logs
```

**응답 200 OK**
```json
[
  {
    "logId": "UUID",
    "deliveryId": "UUID",
    "eventType": "STATUS_CHANGED",
    "status": "HUB_WAITING",
    "description": null,
    "location": null,
    "actorId": "UUID",
    "recordedAt": "2025-05-24T09:05:00"
  },
  {
    "logId": "UUID",
    "deliveryId": "UUID",
    "eventType": "ROUTE_UPDATED",
    "status": null,
    "description": "1번 구간 도착",
    "location": null,
    "actorId": "UUID",
    "recordedAt": "2025-05-24T10:35:00"
  }
]
```

**이벤트 타입**

| eventType | 기록 시점 |
|-----------|---------|
| `STATUS_CHANGED` | 배송 상태 변경 시 |
| `ROUTE_UPDATED` | 배송경로 구간 ARRIVED 시 |
| `CANCELLED` | 배송 삭제(soft delete) 시 |
| `MANAGER_ASSIGNED` | 배송담당자 배정 시 (미구현) |
| `EXCEPTION` | 예외 상황 기록 시 (미구현) |

**권한**: 해당 배송 READ 권한과 동일 (MASTER·HUB_MANAGER·DELIVERY_MANAGER·COMPANY_MANAGER)

## 5. 260526 [2-7 배송담당자 배정] 로직 정비 필요
