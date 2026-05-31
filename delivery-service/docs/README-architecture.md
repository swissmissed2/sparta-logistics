# 스파르타 물류 AI — 전체 아키텍처

---

## 1. 서비스 구성도

```
외부 클라이언트
      │
      ▼
┌─────────────────────────────────────────────┐
│               API Gateway (:8080)           │  JWT 검증 · 라우팅 · X-User-* 헤더 주입
└─────────────────────────────────────────────┘
      │
      ├──────────────────────────────────────────────────────┐
      │                                                      │
      ▼                                                      ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ user-service│  │company-svc  │  │ hub-service │  │order-service│
│  (:19091)   │  │  (:19093)   │  │  (:19092)   │  │  (:19095)   │
└─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘
                                         │                │
                                         │ stock.reserved │ (Kafka)
                                         ▼                ▼
                              ┌──────────────────────────────┐
                              │     delivery-service         │
                              │        (:19096)              │
                              └──────────────────────────────┘
                                         │
                             ai.deadline.calculated (Kafka)
                                         ▲
                              ┌──────────────────────────────┐
                              │     slack-service            │
                              │        (:19097)              │
                              └──────────────────────────────┘

                              ┌──────────────────────────────┐
                              │    product-service           │
                              │        (:19094)              │
                              └──────────────────────────────┘

공통 인프라:
  Discovery Server (Eureka)  :8761
  Config Server              :8888
  PostgreSQL                 :5432
  Redis                      :6379
  Kafka + Zookeeper          :9092 / :2181
  Zipkin                     :9411
```

---

## 2. 서비스별 역할 및 담당 도메인

| 서비스 | 포트 | 주요 역할 | 담당 엔티티 |
|--------|------|----------|-----------|
| **api-gateway** | 8080 | 라우팅, JWT 검증, X-User-* 헤더 주입 | — |
| **discovery-server** | 8761 | Eureka 서비스 등록·발견 | — |
| **config-server** | 8888 | 중앙 설정 관리 (config-repo) | — |
| **user-service** | 19091 | 회원가입·로그인·JWT 발급, 사용자 조회 | 사용자, 역할 |
| **company-service** | 19093 | 업체 등록·관리 | 업체 |
| **hub-service** | 19092 | 허브 관리, 재고 예약, 허브 간 경로 정보 | 허브, 재고 |
| **order-service** | 19095 | 주문 생성·관리, Saga 오케스트레이션 | 주문 |
| **product-service** | 19094 | 상품 등록·조회 | 상품 |
| **delivery-service** | 19096 | 배송 생성·관리, 배송담당자·경로·로그 | 배송, 배송담당자, 배송경로, 이벤트로그 |
| **slack-service** | 19097 | Slack 알림 발송, AI 발송 시한 계산 | 알림 로그 |

---

## 3. 기술 스택

| 영역 | 기술 |
|------|------|
| 언어·프레임워크 | Java 17, Spring Boot 3.x |
| 서비스 디스커버리 | Spring Cloud Netflix Eureka |
| API 게이트웨이 | Spring Cloud Gateway (WebFlux) |
| 중앙 설정 | Spring Cloud Config |
| 서비스 간 통신 (동기) | Spring Cloud OpenFeign |
| 이벤트 스트리밍 (비동기) | Apache Kafka + Zookeeper 7.6.0 |
| 영속성 | Spring Data JPA + PostgreSQL 15 |
| 캐싱·세션 | Redis 7.2 |
| 분산 추적 | Zipkin 3 (Spring Actuator 연동) |
| 인증 | JWT (api-gateway에서 검증, X-User-* 헤더로 전달) |
| 빌드 | Gradle 8 (멀티 모듈) |

---

## 4. [수정중] Kafka 이벤트 흐름 (전체)

```
order.created          ──▶  hub-service        : 재고 예약 트리거
stock.reserved         ──▶  delivery-service   : 배송 생성
delivery.creation.failed ──▶ hub-service        : 재고 복구 (보상 Saga)
delivery.creation.failed ──▶ order-service      : 주문 취소 (보상 Saga)
ai.deadline.calculated ──▶  delivery-service   : 최종 발송 시한 업데이트
```

> 상세 토픽·페이로드 스펙 → [`README-kafka-events.md`](./delivery-service/README-delivery-design.md) 참고

---

## 5. 멀티 모듈 구조

```
sparta-logistics-ai/
├── common/                   # 공통 모듈 (BaseEntity, GlobalExceptionHandler, ApiResponse)
├── api-gateway/
├── discovery-server/
├── config-server/
├── config-repo/              # Git 기반 외부 설정 저장소
├── user-service/
├── company-service/
├── hub-service/
├── order-service/
├── product-service/
├── delivery-service/
├── slack-service/
├── redis/                    # Redis 설정 파일
├── scripts/                  # DB 초기화 스크립트
├── docker-compose.yml
└── build.gradle (루트)
```

---

## 6. 서비스 간 의존 관계 (배송)

```
delivery-service
  ├── → user-service     (Feign: slackId 조회)
  ├── → hub-service      (Feign: 허브 존재 검증)
  └── ← hub-service      (Kafka: stock.reserved 소비)
  └── ← slack-service    (Kafka: ai.deadline.calculated 소비)
  └── → hub-service, order-service (Kafka: delivery.creation.failed 발행)
```

---

## 7. 공통 응답 구조

모든 서비스는 `common` 모듈의 `ApiResponse<T>`를 통해 일관된 응답을 반환합니다.

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

에러 시:
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

## 8. 배송 관련 알림 기능
> 구체적 구현 방식 협의 필요
