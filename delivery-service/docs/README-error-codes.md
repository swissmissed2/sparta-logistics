# Delivery Service — 에러코드 전체 목록

---

## 1. 공통 에러코드 (`CommonErrorCode`)

> 패키지: `common/src/main/java/com/sparta/logistics/common/exception/CommonErrorCode.java`

| 코드           | HTTP 상태 | 메시지 | 발생 시점 |
|--------------|---------|-------|---------|
| `COMMON_001` | 400 Bad Request | 유효성 검증에 실패했습니다. | `@Valid` 검증 실패 (`MethodArgumentNotValidException`) |
| `COMMON_002` | 400 Bad Request | 요청 파라미터 타입이 올바르지 않습니다. | UUID 형식 오류, 잘못된 Enum 값 (`MethodArgumentTypeMismatchException`) |
| `COMMON_003` | 400 Bad Request | 필수 헤더가 누락되었습니다. | X-User-Id 등 required 헤더 누락 (`MissingRequestHeaderException`) |
| `COMMON_004` | 400 Bad Request | 요청 바디를 읽을 수 없습니다. | JSON 형식 오류 (`HttpMessageNotReadableException`) |
| `COMMON_401` | 401 Unauthorized | 인증이 필요합니다. | JWT 미제공·만료 |
| `COMMON_403` | 403 Forbidden | 접근 권한이 없습니다. | 역할 권한 없음 (`BusinessException(FORBIDDEN)`) |
| `COMMON_404` | 404 Not Found | 요청한 리소스를 찾을 수 없습니다. | 범용 Not Found |
| `COMMON_500` | 500 Internal Server Error | 서버 내부 오류가 발생했습니다. | 처리되지 않은 예외 |

---

## 2. 배송 도메인 에러코드 (`DeliveryErrorCode`)

> 패키지: `delivery-service/src/main/java/com/sparta/logistics/delivery/exception/DeliveryErrorCode.java`

### 배송 (Delivery)

| 코드             | HTTP 상태 | 메시지 | 발생 시점                          |
|----------------|---------|-------|--------------------------------|
| `DELIVERY_404` | 404 Not Found | 배송 정보를 찾을 수 없습니다. | 존재하지 않는 deliveryId 조회 시        |
| `DELIVERY_409` | 409 Conflict | 다른 요청에 의해 데이터가 변경되었습니다. 다시 시도해 주세요. | 낙관적 락 충돌(`ObjectOptimisticLockingFailureException`) 또는 DB 제약 위반(`DataIntegrityViolationException`) |
| `DELIVERY_001` | 400 Bad Request | 이미 삭제된 배송입니다. | soft-deleted 배송에 접근 시          |
| `DELIVERY_002` | 400 Bad Request | 허용되지 않는 배송 상태 전이입니다. | `canTransitionTo()` false 반환 시 |

### 배송담당자 (DeliveryManager)

| 코드 | HTTP 상태 | 메시지                    | 발생 시점 |
|------|---------|------------------------|---------|
| `DELIVERY_MGR_404` | 404 Not Found | 배송담당자를 찾을 수 없습니다.      | 존재하지 않는 managerId 조회 시 |
| `DELIVERY_MGR_001` | 400 Bad Request | 이미 삭제된 배송담당자입니다.       | soft-deleted 담당자에 접근 시 |
| `DELIVERY_MGR_002` | 400 Bad Request | 배송 중인 담당자는 삭제할 수 없습니다. | status == WORKING인 담당자 삭제 시도 시 |
| `DELIVERY_MGR_409` | 409 Conflict | 이미 등록된 배송담당자입니다.       | 동일 userId로 중복 등록 시도 시 |

### 배송경로 (DeliveryRoute)

| 코드 | HTTP 상태 | 메시지              | 발생 시점 |
|------|---------|------------------|---------|
| `DELIVERY_ROUTE_404` | 404 Not Found | 배송경로를 찾을 수 없습니다. | 존재하지 않는 routeId 조회 시 |

### 허브 (Hub 연동)

| 코드 | HTTP 상태 | 메시지                         | 발생 시점 |
|------|---------|-----------------------------|---------|
| `DELIVERY_HUB_001` | 400 Bad Request | 존재하지 않는 허브입니다.              | hub-service Feign 허브 존재 검증 실패 시 |
| `DELIVERY_HUB_503` | 503 Service Unavailable | Hub Service를 현재 사용할 수 없습니다. | hub-service Feign 호출 자체 실패 시 |

---

## 3. HTTP 상태코드별 빠른 참조

| HTTP | 코드 목록 | 의미                 |
|------|---------|--------------------|
| **400** | `COMMON_001`, `COMMON_102`, `COMMON_003`, `COMMON_004`, `DELIVERY_001`, `DELIVERY_002`, `DELIVERY_MGR_001`, `DELIVERY_MGR_002`, `DELIVERY_HUB_001` | 클라이언트 요청 오류        |
| **401** | `COMMON_401` | 인증 필요              |
| **403** | `COMMON_403` | 권한 없음              |
| **404** | `COMMON_404`, `DELIVERY_404`, `DELIVERY_MGR_404`, `DELIVERY_ROUTE_404` | 리소스 없음             |
| **409** | `COMMON_409`, `DELIVERY_MGR_409` | 충돌 (낙관적 락, 멱등성 위반) |
| **500** | `COMMON_500` | 서버 오류              |
| **503** | `DELIVERY_HUB_503` | 외부 서비스 불가          |

---

## 4. 응답 예시

**404 배송 없음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "DELIVERY_404",
    "message": "배송 정보를 찾을 수 없습니다."
  }
}
```

**409 낙관적 락 충돌**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_409",
    "message": "다른 요청에 의해 데이터가 변경되었습니다. 다시 시도해 주세요."
  }
}
```

**400 유효성 검증 실패**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_001",
    "message": "[status: 상태 값은 필수입니다.]"
  }
}
```

---

## 5. GlobalExceptionHandler 처리 흐름

```
예외 발생
  │
  ├── BusinessException          → ErrorCode.status + code + message 반환
  ├── MethodArgumentNotValidException → 400 + 필드별 에러 병합
  ├── MethodArgumentTypeMismatchException → 400 TYPE_MISMATCH
  ├── MissingRequestHeaderException → 400 MISSING_REQUEST_HEADER
  ├── HttpMessageNotReadableException → 400 INVALID_REQUEST_BODY
  ├── DataIntegrityViolationException → 409 CONFLICT (safety-net)
  ├── ObjectOptimisticLockingFailureException → 409 CONFLICT
  └── Exception (나머지 전체) → 500 INTERNAL_SERVER_ERROR
```
