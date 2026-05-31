# Delivery Service — 외부 서비스 Feign 계약

delivery-service가 의존하는 외부 서비스와의 인터페이스 계약을 정리합니다.

---

## 1. user-service 연동

### 목적

Kafka `stock.reserved` 소비 시 수령자(receiverId)의 Slack ID를 조회합니다.  
Slack ID가 없으면 `delivery.creation.failed` 이벤트를 발행하고 배송 생성을 중단합니다.

### Feign 클라이언트

```java
// client/UserServiceClient.java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable UUID userId);
}
```

### 응답 DTO

```java
// client/response/UserResponse.java
public record UserResponse(
    UUID userId,
    String slackId   // null 가능 — 미등록 사용자
) {}
```

### 호출 위치 및 트랜잭션

```
DeliveryEventHandler.handleStockReserved()
  │
  ├── [트랜잭션 밖] userServiceClient.getUser(event.receiverId())
  │     → 이유: Feign 대기 시간 동안 DB 커넥션을 점유하지 않기 위해
  │
  └── [트랜잭션 안] deliveryService.createDelivery(event, slackId)
```

### 에러 처리

| 상황 | 처리 |
|------|------|
| user-service 응답 정상, slackId 있음 | 배송 생성 진행 |
| user-service 응답 정상, slackId null | `delivery.creation.failed` 발행 (reason: `SLACK_ID_NOT_FOUND`) |
| user-service 호출 실패 (타임아웃·예외) | `delivery.creation.failed` 발행 (reason: `USER_SERVICE_UNAVAILABLE`) |

### 팀 협의 사항

| 항목 | 상태          | 내용                                 |
|------|-------------|------------------------------------|
| 엔드포인트 경로 | 🔵 문서 확인 필요 | `GET /api/v1/users/{userId}` 인지 체크 |
| 응답 구조 | 🔵 문서 확인 필요 | `data.slackId` 필드 존재 여부 확인 필요      |
| Feign timeout | 🟡 미설정      | 기본값 사용 중 — 운영 전 timeout 설정 권장      |

---

## 2. hub-service 연동

### 목적

`COMPANY_DELIVERY` 타입 배송담당자 생성 시 hubId가 실제 존재하는 허브인지 검증합니다.

### Feign 클라이언트

```java
// client/HubServiceClient.java
@FeignClient(name = "hub-service", fallback = HubServiceClientFallback.class)
public interface HubServiceClient {
    // 200 OK → 허브 존재 확인 완료 / 호출 실패 시 → Fallback으로 위임
    @GetMapping("/api/v1/hubs/{hubId}/exists")
    void checkHubExists(@PathVariable UUID hubId);
}
```

### Fallback

hub-service 호출이 실패(타임아웃·서킷 오픈·네트워크 오류 등)하면 Fallback이 호출됩니다.

```java
// client/HubServiceClientFallback.java
@Component
public class HubServiceClientFallback implements HubServiceClient {
    @Override
    public void checkHubExists(UUID hubId) {
        // 호출 실패 시 항상 HUB_SERVICE_UNAVAILABLE(503) 반환
        throw new BusinessException(DeliveryErrorCode.HUB_SERVICE_UNAVAILABLE);
    }
}
```

### 호출 위치

```java
// DeliveryManagerService.createManager()
if (req.managerType() == DeliveryManagerType.COMPANY_DELIVERY) {
    hubServiceClient.checkHubExists(req.hubId());
    // 정상 응답(200) → 검증 통과
    // 호출 실패(모든 FeignException 포함) → Fallback → BusinessException(HUB_SERVICE_UNAVAILABLE)
}
```

### 에러 처리

| 상황 | 처리 |
|------|------|
| hub-service 200 OK | 검증 통과, 담당자 등록 계속 |
| hub-service 호출 실패 (404·타임아웃·서킷 오픈 등) | Fallback → `DELIVERY_HUB_503` (503 Service Unavailable) |

> **주의**: 현재 구현은 단순 Fallback(`HubServiceClientFallback`)을 사용하므로, hub-service 404(허브 없음)와 서비스 장애를 구분하지 않고 모두 503으로 처리합니다.  
> 404(허브 없음)를 400으로 별도 처리하려면 `FallbackFactory`로 교체 후 원인 예외를 분기해야 합니다.

### 팀 협의 사항

| 항목 | 상태           | 내용    |
|------|--------------|-------|
| 엔드포인트 경로 | 🔵 문서 확인 필요  | `GET /api/v1/hubs/{hubId}/exists` 인지 체크 |
| 응답 구조 | 🔵 문서 확인 필요  | 존재하면 200, 없으면 404 반환 여부 확인              |

---

## 3. Kafka 이벤트 계약 (발행/구독)

### 소비 토픽

#### `stock.reserved`

| 항목 | 내용 |
|------|------|
| 발행자 | hub-service |
| Consumer Group | `delivery-service` |
| 트리거 | 재고 예약 완료 시 |

```json
{
  "orderId": "UUID",
  "receiverId": "UUID",
  "sourceHubId": "UUID", // 이미 기준으로 묶어서 보냄
  "destinationHubId": "UUID",
  "deliveryAddress": "String"
}
```

> 해당 5개 필드를 `stock.reserved` 페이로드에 포함.  
> 주문 1: 상품 N : 배송허브 1~N 구조로,  
> 한 주문에서 여러 상품이 있을 경우, 배송 허브를 기준으로 하나씩 발행

#### `ai.deadline.calculated`

| 항목 | 내용 |
|------|------|
| 발행자 | slack-service (AI 발송 시한 계산 후) |
| Consumer Group | `delivery-service` |
| 트리거 | AI가 최종 발송 시한 계산 완료 시 |

```json
{
  "deliveryId": "UUID",
  "finalDispatchDeadlineAt": "2025-05-25T14:00:00"
}
```

### 발행 토픽

#### `delivery.creation.failed`

| 항목 | 내용 |
|------|------|
| 발행자 | delivery-service |
| 구독자 | hub-service (재고 복구), order-service (주문 취소) |
| 트리거 | 배송 생성 실패 시 (Saga 보상 트랜잭션) |

```json
{
  "orderId": "UUID",
  "reason": "SLACK_ID_NOT_FOUND | USER_SERVICE_UNAVAILABLE | INVALID_HUB_ID"
}
```

> 고민 사항: 1개의 허브에서라도 배송 생성 실패가 된다면, 전체 실패로 간주할지 말지 협의 필요 

---

## 4. 서비스 의존 관계 요약

```
delivery-service
  ├── [Feign] → user-service   GET /api/v1/users/{userId}
  ├── [Feign] → hub-service    GET /api/v1/hubs/{hubId}/exists
  ├── [Kafka 소비] ← hub-service    stock.reserved
  ├── [Kafka 소비] ← slack-service  ai.deadline.calculated
  └── [Kafka 발행] → hub-service, order-service  delivery.creation.failed
```

---

## 5. 미확인 사항

| 항목                                             | 협의 대상 | 내용 | 우선순위  |
|------------------------------------------------|---------|------|-------|
| `GET /api/v1/users/{userId}` 응답                | user-service | `slackId` 필드 포함 여부 | 🔴 필수 |
| 1개의 허브에서라도 배송 생성 실패가 된다면, 전체 실패로 간주할지 말지 협의 필요 | delivery-service | `delivery.creation.failed` 로직 구현 필요 | 🔴 필수 |  
| COMPANY_MANAGER 배송 소유 검증                       | order-service | companyId → orderId 검증 API | 🟢 추후 |