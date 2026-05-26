# 인프라 구성 가이드

---

## 1. 포트 맵

| 컨테이너 / 서비스 | 외부 포트 | 내부 포트 | 용도 |
|------------------|----------|----------|------|
| PostgreSQL | 5432 | 5432 | 공용 DB (스키마 분리) |
| Redis | 6379 | 6379 | JWT Refresh Token, 캐시 |
| Zookeeper | 2181 | 2181 | Kafka 메타데이터 관리 |
| Kafka | 9092 | 9092/29092 | 이벤트 스트리밍 |
| Zipkin | 9411 | 9411 | 분산 추적 UI |
| API Gateway | 8080 | 8080 | 단일 진입점 |
| Discovery Server | 8761 | 8761 | Eureka 대시보드 |
| Config Server | 8888 | 8888 | 중앙 설정 서버 |
| user-service | 19091 | 19091 | |
| hub-service | 19092 | 19092 | |
| company-service | 19093 | 19093 | |
| product-service | 19094 | 19094 | |
| order-service | 19095 | 19095 | |
| delivery-service | 19096 | 19096 | |
| slack-service | 19097 | 19097 | |

---

## 2. docker-compose 구성

`docker-compose.yml`은 **인프라 레이어만** 포함합니다. 마이크로서비스(Spring Boot)는 IDE 또는 `./gradlew bootRun`으로 별도 기동합니다.

### 포함 서비스

```yaml
services:
  postgres   # PostgreSQL 15 — 공용 DB
  redis      # Redis 7.2 — 캐시·세션
  zookeeper  # CP Zookeeper 7.6.0 — Kafka 관리자
  kafka      # CP Kafka 7.6.0 — 이벤트 브로커
  zipkin     # Zipkin 3 — 분산 추적
```

### 네트워크

모든 컨테이너는 `sparta-net` 브리지 네트워크로 연결됩니다.  
서비스 간 통신: `kafka:29092`, `zookeeper:2181`, `postgres:5432`, `redis:6379`

---

## 3. 환경변수 설정

루트 디렉토리에 `.env` 파일을 생성합니다.

```dotenv
# PostgreSQL
POSTGRES_USER=YOUR_NAME
POSTGRES_PASSWORD=YOUR_PASSWORD
POSTGRES_DB=logistics

# Redis (redis.conf에서 참조)
# 별도 설정 불필요 (기본값 사용)

# Kafka (애플리케이션 yaml에서 참조)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Zipkin
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans

# JWT (api-gateway)
JWT_SECRET=YOUR_JWT_SECRET_KEY
```

> `.env` 형식은 `.env.example`에서 확인 가능합니다.

---

## 4. DB 스키마 초기화

PostgreSQL 컨테이너 최초 기동 시 `scripts/init-schemas.sql`이 자동 실행됩니다.  
각 마이크로서비스는 독립 스키마를 사용합니다.

```sql
-- scripts/init-schemas.sql (예시)
CREATE SCHEMA IF NOT EXISTS delivery;
CREATE SCHEMA IF NOT EXISTS hub;
CREATE SCHEMA IF NOT EXISTS orders;
-- ...
```

> 현재 개발 환경: `ddl-auto: create-drop` (애플리케이션 기동 시 테이블 자동 생성/삭제)  
> 운영 전환 시: Flyway 도입 + `ddl-auto: validate` 변경하는 방향을 고려 중

---

## 5. [ 수정 중 ] Kafka 설정 상세

```yaml
# 호스트에서 접속 (로컬 개발): localhost:9092
# 컨테이너 내부 서비스 간 통신: kafka:29092

KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

### Kafka 모니터링 (선택)

Kafdrop을 docker-compose에 추가하면 토픽·메시지를 웹 UI로 확인할 수 있습니다.

```yaml
kafdrop:
  image: obsidiandynamics/kafdrop
  ports:
    - "9000:9000"
  environment:
    KAFKA_BROKERCONNECT: kafka:29092
  depends_on:
    - kafka
  networks:
    - sparta-net
```

접속: http://localhost:9000

---

## 6. Redis 설정

`redis/redis.conf`가 마운트됩니다. 주요 설정:

```conf
# redis/redis.conf
maxmemory 256mb
maxmemory-policy allkeys-lru
```

---

## 7. Zipkin 분산 추적

모든 마이크로서비스는 `spring-actuator` + Micrometer Tracing을 통해 Zipkin에 span을 전송합니다.

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 개발: 전체 샘플링 / 운영: 0.1~0.3 권장
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

Zipkin UI: http://localhost:9411
