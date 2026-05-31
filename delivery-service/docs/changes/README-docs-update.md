# 공통 문서 수정 사항 (ERD.md / SAGA.md)

코드와 문서 간 불일치 항목을 실제 구현에 맞게 수정함.

---

## 1. ERD.md — `p_hub_stock_log.change_type` enum 값 추가

**변경 전**
```
INBOUND|ORDER_DECREASE|CANCEL_RESTORE|RETURN_RESTORE|MANUAL_ADJUST
```

**변경 후**
```
INBOUND|ORDER_RESERVE|ORDER_DECREASE|CANCEL_RESTORE|RETURN_RESTORE|MANUAL_ADJUST|COMPENSATE
```

| 추가 값 | 설명 |
|---------|------|
| `ORDER_RESERVE` | 주문 생성 시 재고 예약 (`order.created` 수신 시점) |
| `COMPENSATE` | 보상 트랜잭션 전용 |

- 근거: `HubStockChangeType.java` 실제 enum 값과 일치시킴

---

## 2. ERD.md — `p_delivery_order_item` 테이블 추가

Delivery Service에 `DeliveryOrderItemEntity`가 구현되어 있었으나 ERD에 누락되어 있었음.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | UUID PK | |
| `delivery_id` | UUID FK | `p_delivery` 참조 |
| `order_item_id` | UUID | 간접참조 → `p_order_item` (nullable, 레거시 이벤트 대비) |
| `product_id` | UUID | |
| `hub_id` | UUID | 출처 허브 ID |
| `quantity` | INTEGER | |

- 용도: 배송에 포함된 주문 상품 매핑. `hub-service` 재고 변경 이력(HubStockLog)과 연결용
- `stock.reserved` 이벤트를 통해 수신한 정보로 생성

---

## 3. SAGA.md — `hub.deleted` 토픽 추가

**변경 전**: Kafka 토픽 목록에 없음

**변경 후**: 토픽 테이블에 행 추가

| 토픽 | Publisher | Subscriber | 패턴 |
|------|-----------|-----------|------|
| `hub.deleted` | HubService | 관련 서비스 | 허브 삭제 cascade |

- 근거: `KafkaTopics.java`에 `HUB_DELETED = "hub.deleted"` 상수 존재

---

## 4. SAGA.md — Step 1-7 `HubStockChangeType` 수정

**변경 전**
```
처리 내용: orderItems 순회하여 각 상품 reserved - N, HubStockChangeType.DELIVERY_STARTED 이력 기록
비고: HubStockChangeType에 DELIVERY_STARTED 타입 추가 필요
```

**변경 후**
```
처리 내용: orderItems 순회하여 각 상품 reserved - N, HubStockChangeType.ORDER_DECREASE 이력 기록
```

- 근거: `HubStockLockHelper.java`에서 `delivery.started` 수신 시 `ORDER_DECREASE` 타입 사용 (이미 구현 완료)
- `DELIVERY_STARTED` 타입은 코드에 존재하지 않으므로 "추가 필요" 비고 제거
