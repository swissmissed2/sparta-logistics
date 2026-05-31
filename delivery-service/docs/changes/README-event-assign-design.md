# 배송 생성 이벤트 & 배차 설계 (Phase 3)

> 관련 이슈: #135

---

## 1. 배경 및 목적

| 항목 | 내용 |
|--|--|
| 대상 브랜치 | `refactor/135-delivery-ai-api` |
| 변경 범위 | 배송 생성 Kafka 흐름 개선, 배차 API 신규 구현 |

기존 `stock.reserved` 핸들러에 존재하던 8가지 문제를 해결하고,  
`delivery.created` 이벤트 발행 및 배차(담당자 배정) API를 완성한다.

---

## 2. 전체 데이터 흐름

```
[order-service]
  stock.reserved 발행 — sourceHubId 기준으로 이미 그룹핑된 단일 이벤트
  { orderId, receiverId, sourceHubId, destinationHubId, deliveryAddress, orderItems[] }
  │
  ▼
[DeliveryEventHandler.handleStockReserved()]
  ├─ JSON 역직렬화 실패 → log.error only  (orderId 파싱 불가)
  │
  ├─ Feign (트랜잭션 밖): user-service → slackId 조회
  │     └─ data() == null → publishCreationFailed("SLACK_ID_NOT_FOUND")
  │     └─ 예외 → publishCreationFailed("USER_SERVICE_UNAVAILABLE")
  │
  ├─ Feign (트랜잭션 밖): hub-service → 허브 간 경로 구간 조회
  │     └─ 예외 → publishCreationFailed("HUB_SERVICE_UNAVAILABLE")
  │
  └─ try {
       단일 트랜잭션: DeliveryEntity + DeliveryRouteEntity[] 저장
       커밋 후: delivery.created 발행
     } catch { publishCreationFailed("CREATE_FAILED") }

[delivery.created 수신 측]
  ├─ AI-service: 발송 시한 계산 → ai.deadline.calculated 회신
  └─ 배차: POST /api/v1/deliveries/{id}/assign (수동, 추후 Kafka consumer 전환 가능)
               → DeliveryAssignmentService.assignManagers()
                    findNextAssignee() → assign() → assignManager()
```

---

## 3. 해결된 문제 목록

| # | 문제 | 해결 방법 |
|--|--|--|
| 1 | 역직렬화 실패 시 메시지 소실 | log.error only (orderId 파싱 불가로 publishCreationFailed 불가) |
| 2 | createDelivery 예외 시 publishCreationFailed 미호출 | try/catch 추가 |
| 3 | `data()` null → NPE → USER_SERVICE_UNAVAILABLE 오분류 | `response.data() == null` 명시적 체크 |
| 4 | receiverSlackId NOT NULL | 현재 구조 유지 (생성 시 1회 Feign, 이후 user-service 불필요) |
| 5 | delivery.created 이벤트 미발행 | `publishCreated()` 추가 |
| 6 | 멱등성 키 — 다중 허브 주문 중복 방지 | `orderId + sourceHubId` 쌍으로 변경 |
| 7 | 배차 로직 미구현 | `DeliveryAssignmentService` + `POST /assign` 추가 |
| 8 | DeliveryRoute 생성 로직 없음 | stock.reserved 처리 시 hub-service Feign 후 즉시 생성 |

---

## 4. 멱등성 키 변경

```
변경 전: existsByOrderId(orderId)
변경 후: existsByOrderIdAndSourceHubId(orderId, sourceHubId)
```

**이유:** 하나의 주문에 여러 출발 허브가 있으면 order-service가 sourceHubId별로 `stock.reserved`를
N개 발행한다. orderId 단독 체크 시 두 번째 이벤트부터 중복으로 막혀 배송이 1개만 생성된다.

---

## 5. DeliveryRoute 생성 시점

```
stock.reserved 처리 중 (트랜잭션 밖):
  hub-service.getRouteSegments(sourceHubId, destinationHubId)
  → List<HubRouteSegmentResponse> 반환

단일 트랜잭션:
  DeliveryEntity 저장
  for each segment: DeliveryRouteEntity 저장 (실제 estimatedDistance, estimatedDuration)
```

hub-service Feign 실패 → `publishCreationFailed("HUB_SERVICE_UNAVAILABLE")` 발행 후 중단.

**⚠ hub-service 팀 협의 필요:** `GET /api/v1/hub-routes?sourceHubId=&destinationHubId=` 엔드포인트 응답 구조 확정.

---

## 6. 배차 API

```
POST /api/v1/deliveries/{deliveryId}/assign
권한: MASTER, HUB_MANAGER (자기 허브 배송만)
```

`DeliveryAssignmentService.assignManagers(deliveryId, actorId, role, hubId)`:
1. 배송 조회 + 권한 체크
2. `routeRepository.findAllByDelivery_IdOrderBySequenceAsc(deliveryId)`
3. route별 타입 판별 (HUB_TO_HUB → HUB_DELIVERY, HUB_TO_COMPANY → COMPANY_DELIVERY)
4. `findNextAssignee(sourceHubId, type, IDLE)` — 라운드 로빈
5. `manager.assign()` + `route.assignManager()` + `delivery.assignCompanyDeliveryManager()`
6. `DeliveryLogEntity` 저장 (MANAGER_ASSIGNED)

**추후 자동화:** `delivery.created` Kafka consumer에서 `assignManagers()` 호출 시 메서드 변경 불필요.

---

## 7. Kafka 전략

- **단일 트랜잭션:** N개 route와 배송 함께 저장 (하나 실패 시 전체 롤백)
- **Kafka 발행 실패:** retry + 컨슈머에서 처리된 `event_id` DB 저장으로 중복 방지
- **추후:** 다른 도메인과 공통 outbox 모듈로 전환

---

## 8. 변경된 파일 목록

| 파일 | 변경 유형 |
|--|--|
| `dto/event/StockReservedEventDto.java` | 수정 (orderItems 추가) |
| `dto/event/StockReservedItemPayload.java` | 신규 |
| `dto/event/DeliveryCreatedEvent.java` | 신규 |
| `client/response/HubRouteSegmentResponse.java` | 신규 |
| `client/HubServiceClient.java` | 수정 (getRouteSegments 추가) |
| `client/HubServiceClientFallback.java` | 수정 (fallback 추가) |
| `repository/DeliveryRepository.java` | 수정 (existsByOrderIdAndSourceHubId 추가) |
| `infrastructure/event/DeliveryEventHandler.java` | 수정 (예외 처리, hub Feign, try/catch) |
| `infrastructure/event/DeliveryEventPublisher.java` | 수정 (publishCreated 추가) |
| `service/DeliveryService.java` | 수정 (멱등성 키, route 생성, publishCreated) |
| `service/DeliveryAssignmentService.java` | 신규 |
| `exception/DeliveryErrorCode.java` | 수정 (에러코드 추가) |
| `controller/DeliveryController.java` | 수정 (POST /assign 추가) |
