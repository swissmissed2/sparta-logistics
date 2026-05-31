# Delivery Service — 설계 결정 사항

배송 생성 API 구현 과정에서 팀 간 협의된 설계 결정 사항입니다.

---

## 1. Kafka 이벤트 흐름

```
[order-service]
   │
   ▼ order.created (발행)
[hub-service]  ← 재고 예약
   │
   ▼ stock.reserved (발행)
[delivery-service]  ← 배송 생성  ← 이번 구현 범위
   │
   ▼ delivery.creation.failed (실패 시 발행)
[hub-service]       ← 재고 예약 취소 (보상 트랜잭션, 미구현)
[order-service]     ← 주문 상태 CANCELLED (보상 트랜잭션, 미구현)

[notification-service(slack-service)]
   │  AI 발송 시한 계산 후
   ▼ ai.deadline.calculated (발행)
[delivery-service]  ← finalDispatchDeadlineAt 업데이트
```

### 토픽 정의

| 토픽 | 발행 서비스 | 구독 서비스 | 시점 |
|------|-----------|-----------|------|
| `order.created` | order-service | hub-service | 주문 생성 시 |
| `stock.reserved` | hub-service | **delivery-service** | 재고 예약 완료 시 |
| `delivery.creation.failed` | **delivery-service** | hub-service, order-service | 배송 생성 실패 시 |
| `ai.deadline.calculated` | notification-service | **delivery-service** | AI 발송 시한 계산 완료 시 |

---

## 2. 이벤트 DTO 전략 — 서비스별 독립 DTO

**결정**: 공유 모듈(common)에 이벤트 DTO를 두지 않고, 각 서비스가 독립적으로 DTO를 정의합니다.

**이유**: common 모듈에 공유 DTO를 두면 한 서비스 변경이 모든 서비스에 영향을 미칩니다. 독립 DTO는 각 서비스가 자신이 필요한 필드만 정의하여 결합도를 낮춥니다.

**구조 예시**:
```
hub-service:     StockReservedEvent.java         (발행용)
delivery-service: StockReservedEventDto.java     (수신용, 동일 구조지만 별도 정의)
```

**수정이 필요한 경우**: hub-service의 실제 발행 이벤트 필드명이 변경되면 `StockReservedEventDto.java`의 필드명도 동기화 필요.

---

## 3. stock.reserved 이벤트 — delivery-service가 필요한 필드

hub-service 팀에 아래 필드가 `stock.reserved` 이벤트에 포함되도록 협의 필요:

```java
{
  "orderId": "UUID",
  "receiverId": "UUID",        // 주문자 userId (알림 수신자)
  "sourceHubId": "UUID",       // 출발 허브 (requesterCompany.hubId)
  "destinationHubId": "UUID",  // 도착 허브 (receiverCompany.hubId)
  "deliveryAddress": "String"  // receiverCompany.address 스냅샷
}
```

> `CompanyResponse.hubId` 및 `CompanyResponse.address` 필드는 order-service에서 이미 사용 중 (`CompanyResponse.java`).

---

## 4. 트랜잭션 단위

**결정**: Feign 호출은 트랜잭션 밖, DB 저장만 `@Transactional`

```
Kafka 소비 (트랜잭션 없음)
  │
  ├─ 검증 (sourceHubId, destinationHubId null 체크)
  │
  ├─ user-service Feign 호출 → slackId 조회  ← 트랜잭션 시작 전
  │
  └─ @Transactional deliveryService.createDelivery()
       └─ DeliveryRepository.save()
```

**이유**: 외부 HTTP 호출(Feign)을 DB 트랜잭션 안에 포함하면 커넥션 풀이 Feign 대기 시간 동안 점유됩니다. DB 커넥션은 최소 범위로 유지합니다.

**Kafka 재시도 안전성**: user-service Feign 호출은 멱등 조회(`GET`)이므로 Kafka 재시도 시 재호출해도 안전합니다.

---

## 5. slackId null 처리 정책

**결정**: slackId가 없으면 `delivery.creation.failed` 이벤트 발행 후 배송 생성 중단

**발생 시나리오**:
- user-service 일시 장애 (Feign 타임아웃/예외)
- 해당 사용자가 슬랙 미등록인 경우

**현재 상태**: `delivery.creation.failed` 이벤트를 발행만 하고 hub/order는 아직 구독하지 않음. 추후 각 팀이 보상 트랜잭션(재고 복구, 주문 취소) 구독 추가 예정.

```java
// delivery.creation.failed 이벤트 구조
{
  "orderId": "UUID",
  "reason": "SLACK_ID_NOT_FOUND" | "USER_SERVICE_UNAVAILABLE" | "INVALID_HUB_ID"
}
```

---

## 6. sourceHubId / destinationHubId null 처리

**결정**: null이면 `delivery.creation.failed` 이벤트 발행 후 배송 생성 중단

**이유**: 배송 경로 정보 없이 배송 엔티티를 저장하면 추후 배송 경로 계산 로직에서 데이터 이상이 발생합니다.

---

## 7. Consumer Group ID 기준

**결정**: `groupId = "delivery-service"` (서비스명 기준)

| groupId | 의미 |
|---------|------|
| 같은 그룹 내 여러 인스턴스 | 파티션을 나눠 처리 (scale-out) |
| 다른 그룹의 같은 토픽 구독 | 각자 모든 메시지 수신 |

hub-service가 `delivery.creation.failed`를 구독할 때는 `groupId = "hub-service"`로 별도 설정.

---

## 8. Kafka 로그 모니터링

팀 환경에서는 **Kafdrop** 추가 권장:

```yaml
# docker-compose.yml에 추가
kafdrop:
  image: obsidiandynamics/kafdrop
  ports:
    - "9000:9000"
  environment:
    KAFKA_BROKERCONNECT: kafka:9092
  depends_on:
    - kafka
```

Kafdrop은 기존 Zookeeper와 독립적으로 Kafka 브로커(`kafka:9092`)에 직접 연결합니다.  
Zookeeper는 Kafka 자체를 구동하기 위한 것, Kafdrop은 Kafka를 모니터링하기 위한 도구입니다.

---

## 9. 외부 서비스 협의 사항

| 협의 대상 | 필요한 내용 |
|----------|-----------|
| **hub-service 팀** | `stock.reserved` 이벤트에 `orderId, receiverId, sourceHubId, destinationHubId, deliveryAddress` 포함 |
| **hub-service 팀** | `delivery.creation.failed` 이벤트 구독 및 재고 취소 로직 구현 (Saga 보상) |
| **order-service 팀** | `delivery.creation.failed` 이벤트 구독 및 주문 상태 변경 로직 구현 (Saga 보상) |
| **user-service 팀** | `GET /api/v1/users/{userId}` 응답에 `slackId` 필드 포함 여부 확인 |
| **notification-service 팀** | `ai.deadline.calculated` 이벤트 발행 시 `deliveryId, finalDispatchDeadlineAt` 포함 |

---

## 10. DB 스키마 관리

현재: `spring.jpa.hibernate.ddl-auto: create-drop` (개발 환경)  
추후: 배포 환경 준비 시 Flyway 도입 권장 (`V1__init.sql` 형식, 적용된 파일 수정 불가 규칙)
