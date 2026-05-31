# ERD

> 서비스별 독립 DB 구조 (MSA)

## 목차

- [User Service](#user-service)
- [Hub Service](#hub-service)
- [Company Service](#company-service)
- [Product Service](#product-service)
- [Order Service](#order-service)
- [Delivery Service](#delivery-service)
- [Slack Service](#slack-service)
---

## User Service

```mermaid
erDiagram
    p_user {
        UUID      id              PK
        VARCHAR   username        UK  "4~10자, 영문소문자·숫자"
        VARCHAR   password            "BCrypt 암호화"
        VARCHAR   name
        VARCHAR   email           UK
        VARCHAR   slack_id
        VARCHAR   role                "MASTER|HUB_MANAGER|DELIVERY_MANAGER|COMPANY_MANAGER"
        VARCHAR   status              "PENDING|APPROVED|REJECTED"
        UUID      hub_id              "간접참조 → p_hub (허브관리자·배송담당자 시 필수)"
        UUID      company_id          "간접참조 → p_company (업체담당자 시 필수)"
        TIMESTAMP last_login_at
    }
```

---

## Hub Service

```mermaid
erDiagram
    p_hub {
        UUID    id              PK
        VARCHAR name            UK  "허브명 (전국 유일)"
        VARCHAR address
        DECIMAL latitude            "DECIMAL(9,6)"
        DECIMAL longitude           "DECIMAL(9,6)"
        VARCHAR status              "ACTIVE|INACTIVE"
    }

    p_hub_route {
        UUID    id                  PK
        UUID    source_hub_id       FK  "출발 허브"
        UUID    destination_hub_id  FK  "도착 허브"
        DECIMAL distance                "km, DECIMAL(10,3)"
        INTEGER duration                "분"
    }

    p_hub_stock {
        UUID    id          PK
        UUID    hub_id      FK
        UUID    product_id      "간접참조 → p_product  ※ UNIQUE(hub_id, product_id)"
        INTEGER available       "주문 가능 수량"
        INTEGER reserved        "예약 수량"
        BIGINT  version         "낙관적 락"
    }

    p_hub_stock_log {
        UUID    id              PK
        UUID    hub_stock_id    FK
        UUID    order_item_id       "간접참조 → p_order_item"
        UUID    delivery_id         "간접참조 → p_delivery"
        INTEGER change_quantity     "+증가 / -감소"
        INTEGER before_quantity
        INTEGER after_quantity
        VARCHAR change_type         "INBOUND|ORDER_RESERVE|ORDER_DECREASE|CANCEL_RESTORE|RETURN_RESTORE|MANUAL_ADJUST|COMPENSATE"
    }

    p_hub         ||--o{ p_hub_route     : "source_hub_id"
    p_hub         ||--o{ p_hub_route     : "destination_hub_id"
    p_hub         ||--o{ p_hub_stock     : "hub_id"
    p_hub_stock   ||--o{ p_hub_stock_log : "hub_stock_id"
```

---

## Company Service

```mermaid
erDiagram
    p_company {
        UUID    id          PK
        VARCHAR name
        VARCHAR type            "PRODUCER|RECEIVER"
        UUID    hub_id          "간접참조 → p_hub"
        VARCHAR address
        DECIMAL latitude        "DECIMAL(10,7)"
        DECIMAL longitude       "DECIMAL(10,7)"
        VARCHAR status          "ACTIVE|INACTIVE"
    }
```

---

## Product Service

```mermaid
erDiagram
    p_product {
        UUID    id          PK
        VARCHAR name
        UUID    company_id      "간접참조 → p_company"
        UUID    hub_id          "간접참조 → p_hub"
        BIGINT  price           "기본 단가 (₩)"
        TEXT    description
        VARCHAR status          "AVAILABLE|OUT_OF_STOCK|HIDDEN|DISCONTINUED"
    }
```

---

## Order Service

```mermaid
erDiagram
    p_order {
        UUID id PK
        UUID requester_company_id "요청 업체 ID (간접 참조)"
        UUID receiver_company_id "수령 업체 ID (간접 참조)"
        UUID requester_user_id "주문자 ID (간접 참조)"
        VARCHAR_20 status "PENDING|ACCEPTED|CANCELLING|IN_DELIVERY|COMPLETED|CANCELLED"
        BIGINT total_amount
        TIMESTAMP due_date "납품 기한"
        TEXT request_memo
        TIMESTAMP cancelled_at
        UUID cancelled_by
        TEXT cancel_reason
        UUID delivery_id "배송 ID (간접 참조)"
        BIGINT version "낙관적 락"
    }

    p_order_item {
        UUID id PK
        BIGINT version "낙관적 락"
        UUID order_id FK
        UUID product_id "상품 ID (간접 참조)"
        VARCHAR_150 product_name "주문 시점 스냅샷"
        BIGINT unit_price "주문 시점 스냅샷"
        INTEGER quantity
        BIGINT sub_total "unit_price x quantity"
        UUID hub_id "상품 출처 허브 ID (간접 참조)"
    }

    p_order_delivery {
        UUID id PK
        UUID order_id "주문 ID (간접 참조)"
        UUID delivery_id "배송 ID (간접 참조)"
    }

    p_product_stock_snapshot {
        UUID id PK
        UUID product_id UK "상품 ID (간접 참조)"
        UUID hub_id "소속 허브 ID (간접 참조)"
        INTEGER available "캐싱된 주문 가능 수량"
        BIGINT hub_stock_version "동기화 판단용 버전"
        TIMESTAMP synced_at "마지막 동기화 일시"
    }

    p_order ||--o{ p_order_item : "주문 상세"
    p_order ||--o{ p_order_delivery : "배송 매핑"
```

---

## Delivery Service

```mermaid
erDiagram
    p_delivery_manager {
        UUID      id                  PK  "= p_user.id (배송담당자 사용자 ID)"
        UUID      hub_id                  "간접참조 → p_hub"
        VARCHAR   slack_id               "비정규화: User 서비스 호출 없이 알림 발송용"
        VARCHAR   manager_type           "HUB_DELIVERY|COMPANY_DELIVERY"
        INTEGER   delivery_sequence      "라운드로빈 순번"
        TIMESTAMP last_assigned_at
        VARCHAR   status                 "IDLE|WORKING|INACTIVE|WITHDRAWN"
    }

    p_delivery {
        UUID      id                          PK
        UUID      order_id                    UK  "간접참조 → p_order"
        VARCHAR   status                          "CREATED|HUB_WAITING|HUB_MOVING|DESTINATION_HUB_ARRIVED|OUT_FOR_DELIVERY|COMPLETED|CANCELLED"
        UUID      source_hub_id                   "간접참조 → p_hub"
        UUID      destination_hub_id              "간접참조 → p_hub"
        UUID      current_hub_id                  "간접참조 → p_hub"
        VARCHAR   delivery_address
        UUID      receiver_id                     "간접참조 → p_user (NULL=requester)"
        VARCHAR   receiver_slack_id
        UUID      company_delivery_manager_id FK
        TIMESTAMP final_dispatch_deadline_at      "AI 산출 최종 발송 시한"
        TIMESTAMP started_at
        TIMESTAMP completed_at
    }

    p_delivery_route {
        UUID      id                      PK
        UUID      delivery_id             FK
        INTEGER   sequence                    "경로 순번"
        VARCHAR   route_type                  "HUB_TO_HUB|HUB_TO_COMPANY"
        UUID      source_hub_id               "간접참조 → p_hub"
        UUID      destination_hub_id          "간접참조 → p_hub (HUB_TO_HUB 시 필수)"
        DECIMAL   estimated_distance          "km, DECIMAL(10,3)"
        INTEGER   estimated_duration          "분"
        DECIMAL   actual_distance             "km, DECIMAL(10,3)"
        INTEGER   actual_duration             "분"
        VARCHAR   status                      "WAITING|IN_TRANSIT|ARRIVED|CANCELLED"
        UUID      hub_delivery_manager_id     "간접참조 → p_delivery_manager (NULL 허용, 물리 FK 미설정)"
        TIMESTAMP started_at
        TIMESTAMP arrived_at
    }

    p_delivery_log {
        UUID      id              PK
        UUID      delivery_id     FK
        VARCHAR   event_type          "MANAGER_ASSIGNED|ROUTE_UPDATED|STATUS_CHANGED|CANCELLED|EXCEPTION"
        VARCHAR   status              "STATUS_CHANGED 시 p_delivery.status 값"
        VARCHAR   description
        VARCHAR   location
        UUID      actor_id            "간접참조 → p_user (시스템 자동 처리=NULL)"
        TIMESTAMP recorded_at
    }

    p_delivery_order_item {
        UUID      id              PK
        UUID      delivery_id     FK
        UUID      order_item_id       "간접참조 → p_order_item (nullable, 레거시 이벤트 대비)"
        UUID      product_id
        UUID      hub_id              "출처 허브 ID"
        INTEGER   quantity
    }

    p_delivery_manager |o--o{ p_delivery              : "company_delivery_manager_id"
    p_delivery         ||--o{ p_delivery_route         : "delivery_id"
    p_delivery         ||--o{ p_delivery_log           : "delivery_id"
    p_delivery         ||--o{ p_delivery_order_item    : "delivery_id"
```

---

## Slack Service

```mermaid
erDiagram
    p_slack_message {
        UUID      id                  PK
        VARCHAR   receiver_slack_id
        TEXT      message
        VARCHAR   message_type            "MANUAL|DEADLINE_ALERT|DAILY_ROUTINE"
        VARCHAR   status                  "PENDING|SENT|FAILED"
        VARCHAR   related_type            "DELIVERY|DELIVERY_MANAGER (polymorphic)"
        UUID      related_id              "polymorphic 연관 ID"
        UUID      sender_id               "간접참조 → p_user (시스템 자동 발송=NULL)"
        INTEGER   retry_count
        TIMESTAMP sent_at
        VARCHAR   slack_ts                "Slack API 반환 메시지 타임스탬프 (수정·삭제용)"
        VARCHAR   slack_channel_id        "Slack API 반환 채널 ID (수정·삭제용)"
    }

    p_ai_log {
        UUID      id                  PK
        UUID      slack_message_id    FK
        UUID      order_id                "간접참조 → p_order"
        VARCHAR   request_type            "DEADLINE|ROUTE"
        TEXT      request_content
        TEXT      response_content
        TEXT      system_prompt           "버전 관리용"
        TIMESTAMP final_deadline_at       "DEADLINE 타입 시 저장"
        VARCHAR   status                  "PENDING|SUCCESS|RETRY|FAILED"
    }

    p_slack_message |o--o{ p_ai_log : "slack_message_id"
```