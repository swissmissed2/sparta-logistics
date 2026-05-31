# sparta-logistics

> 주문 생성부터 배송 완료까지의 흐름을 Kafka 기반 Saga 패턴으로 처리하는 Spring Boot MSA 물류 관리 플랫폼 프로젝트입니다.

## 팀원 소개

| 프로필 | 이름 | 역할 및 담당 도메인 | GitHub |
| :---: | :---: | :--- | :---: |
| <img src="https://github.com/euneuneun.png" width="80" height="80"/> | **김다은** | 인프라 & 사용자 | [![GitHub](https://img.shields.io/badge/GitHub-euneuneun-181717?style=flat&logo=github)](https://github.com/euneuneun) |
| <img src="https://github.com/swissmissed2.png" width="80" height="80"/> | **김승현** | 리더 & 허브 | [![GitHub](https://img.shields.io/badge/GitHub-swissmissed2-181717?style=flat&logo=github)](https://github.com/swissmissed2) |
| <img src="https://github.com/qkrwns1478.png" width="80" height="80"/> | **박준식** | 주문 | [![GitHub](https://img.shields.io/badge/GitHub-qkrwns1478-181717?style=flat&logo=github)](http://github.com/qkrwns1478) |
| <img src="https://github.com/dahye1111.png" width="80" height="80"/> | **이다혜** | 업체 & 상품 | [![GitHub](https://img.shields.io/badge/GitHub-dahye1111-181717?style=flat&logo=github)](http://github.com/dahye1111) |
| <img src="https://github.com/start-ha.png" width="80" height="80"/> | **장하영** | 인프라 & Slack + AI | [![GitHub](https://img.shields.io/badge/GitHub-start--ha-181717?style=flat&logo=github)](https://github.com/start-ha) |
| <img src="https://github.com/look516.png" width="80" height="80"/> | **조아영** | 배송 | [![GitHub](https://img.shields.io/badge/GitHub-look516-181717?style=flat&logo=github)](http://github.com/look516) |

## 서비스 구성

| 서비스 | 역할 | 포트 |
|---|---|---|
| discovery-server | Eureka 서비스 디스커버리 | 8761 |
| config-server | 중앙 설정 관리 | 8888 |
| api-gateway | 라우팅, JWT 검증 | 8080 |
| user-service | 회원가입/로그인/사용자 관리 | 19091 |
| hub-service | 허브/재고/경로 관리 | 19092 |
| company-service | 업체 관리 | 19093 |
| product-service | 상품 관리 | 19094 |
| order-service | 주문 관리, 취소 오케스트레이터 | 19095 |
| delivery-service | 배송 생성/관리 | 19096 |
| slack-service | Slack 알림, AI 발송 시한 계산 | 19097 |

## 기술 스택

| 분류 | 기술                                                                                                                                                                                                                                                                                                                                 |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Language / Framework | ![Java 17](https://img.shields.io/badge/Java_17-007396?style=flat&logo=java&logoColor=white) ![Spring Boot 3](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat&logo=springboot&logoColor=white) ![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-6DB33F?style=flat&logo=spring&logoColor=white)  ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)|
| Service Mesh | ![Netflix Eureka](https://img.shields.io/badge/Netflix_Eureka-20232A?style=flat&logo=netflix&logoColor=white) ![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-6DB33F?style=flat&logo=spring&logoColor=white) ![OpenFeign](https://img.shields.io/badge/OpenFeign-20232A?style=flat) |
| Messaging | ![Apache Kafka 7.6](https://img.shields.io/badge/Apache_Kafka_7.6-231F20?style=flat&logo=apachekafka&logoColor=white)                                                                                                                                                                                                       |
| Database | ![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL_15-4169E1?style=flat&logo=postgresql&logoColor=white) ![Redis 7.2](https://img.shields.io/badge/Redis_7.2-DC382D?style=flat&logo=redis&logoColor=white)                                                                                                     |
| Observability | ![Zipkin 3](https://img.shields.io/badge/Zipkin_3-F25E22?style=flat)                                                                                                                                                                                                                                                        |
| Build | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat&logo=gradle&logoColor=white)                                                                                                                                                                                                                       |

## 아키텍처 설계도

<details>
    <summary>더보기</summary>
    <img src="./docs/architecture.png" alt="architecture"/>
</details>

## 문서

| 문서 | 내용 |
|---|---|
| [docs/ERD.md](docs/ERD.md) | ERD 다이어그램 |
| [docs/SAGA.md](docs/SAGA.md) | Saga 패턴 의사결정, Kafka 토픽 전체 목록, 시퀀스 다이어그램 |
| [docs/auth](docs/auth) | 인증/인가 흐름 |

---

## 실행 방법

### 1단계: 환경변수 설정

루트 디렉터리에 `.env` 파일을 생성합니다.

```dotenv
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password
POSTGRES_DB=your_database
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
JWT_SECRET=your_jwt_secret
GEMINI_API_KEY=your_gemini_api_key
```

### 2단계: 인프라 기동

```bash
docker compose up -d
docker compose ps
```

정상 기동 시 `sparta-postgres`, `sparta-redis`, `sparta-zookeeper`, `sparta-kafka`, `sparta-zipkin` 이 모두 `running` 상태여야 합니다.

### 3단계: 서비스 기동

의존성 순서에 따라 기동합니다.

```
1. discovery-server
2. config-server
3. api-gateway
4. 비즈니스 서비스 (순서 무관)
   user-service, company-service, hub-service,
   product-service, order-service, delivery-service, slack-service
```

Gradle로 기동하는 경우:

```bash
./gradlew :discovery-server:bootRun &
./gradlew :config-server:bootRun &
./gradlew :api-gateway:bootRun &
./gradlew :order-service:bootRun &
# 나머지 서비스도 동일하게 실행
```

### 4단계: 기동 확인

| 확인 항목 | URL |
|---|---|
| Eureka 대시보드 (서비스 등록 확인) | http://localhost:8761 |
| API Gateway 헬스체크 | http://localhost:8080/actuator/health |
| Zipkin 분산 추적 | http://localhost:9411 |
| Swagger UI (Gateway 통합) | http://localhost:8080/swagger-ui.html |

---

## Saga 흐름 요약

주문 생성은 **Choreography Saga**, 주문 취소는 **Orchestration Saga** (Order Service 내장 오케스트레이터) 로 처리합니다.

```mermaid
sequenceDiagram
    participant O as OrderService
    participant H as HubService
    participant D as DeliveryService
    participant S as SlackService

    Note over O,S: 주문 생성 (Choreography)
    O->>H: order.created
    H->>D: stock.reserved
    D->>O: delivery.created
    D->>S: delivery.created
    S->>D: ai.deadline.calculated
    D->>H: delivery.started
```

```mermaid
sequenceDiagram
    participant O as OrderService (Orchestrator)
    participant D as DeliveryService
    participant H as HubService

    Note over O,H: 주문 취소 (Orchestration)
    O->>D: cancel.delivery.command
    D->>O: delivery.cancelled.ack
    O->>H: restore.stock.command
    H->>O: stock.restored.ack
    O->>O: p_order → CANCELLED
```

자세한 Kafka 토픽 목록과 보상 트랜잭션 흐름은 [docs/SAGA.md](docs/SAGA.md)를 참고하세요.

## 권한 체계

| 역할 | 코드 | 설명 |
|---|---|---|
| 마스터 | `MASTER` | 전체 시스템 관리, 모든 CRUD 가능 |
| 허브 관리자 | `HUB_MANAGER` | 담당 허브 소속 데이터 관리 |
| 배송 담당자 | `DELIVERY_MANAGER` | 허브/업체 배송 담당 |
| 업체 담당자 | `COMPANY_MANAGER` | 소속 업체 정보 및 주문 관리 |

API 요청 시 Gateway가 JWT를 검증하고 `X-User-Id`, `X-User-Role`, `X-User-HubId`(선택), `X-User-CompanyId`(선택) 헤더를 하위 서비스에 전달합니다.

## 팀 문서

자세한 설계 사항은 [SA문서](https://www.notion.so/teamsparta/SA-3602dc3ef514809eb4defc610e32fa9e#3602dc3ef51480e6af10e8648e8c664d)를 참고하세요.
