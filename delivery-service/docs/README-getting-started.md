# 로컬 개발환경 시작 가이드

---

## 사전 요구사항

| 도구 | 버전 | 확인 명령 |
|------|------|---------|
| JDK | 17 이상 | `java -version` |
| Docker Desktop | 최신 | `docker --version` |
| Docker Compose | V2 | `docker compose version` |
| Git | — | `git --version` |

---

## 1단계: 저장소 클론

```bash
git clone https://github.com/<org>/sparta-logistics-ai.git
cd sparta-logistics-ai
```

---

## 2단계: 환경변수 파일 생성

루트 디렉토리에 `.env` 파일을 생성합니다.

```dotenv
POSTGRES_USER=YOUR_NAME
POSTGRES_PASSWORD=YOUR_PASSWORD
POSTGRES_DB=logistics
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
JWT_SECRET=YOUR_JWT_SECRET_KEY
```

> .env.example 참고

---

## 3단계: 인프라 기동

```bash
# 인프라 컨테이너 전체 기동 (백그라운드)
docker compose up -d

# 기동 확인
docker compose ps
```

### 예상 출력

```
NAME                STATUS
sparta-postgres     running
sparta-redis        running
sparta-zookeeper    running
sparta-kafka        running
sparta-zipkin       running
```

---

## 4단계: 서비스 기동 순서

의존성 때문에 반드시 **아래 순서대로** 기동합니다.

```
1. discovery-server  (Eureka — 모든 서비스가 여기에 등록)
2. config-server     (중앙 설정 제공)
3. api-gateway       (라우팅 진입점)
4. 비즈니스 서비스   (순서 무관)
   user-service, company-service, hub-service,
   order-service, product-service, delivery-service, slack-service
```

### IntelliJ에서 기동

각 서비스의 `*Application.java`를 Run하거나, Run Configurations에 서비스를 추가합니다.

### Gradle로 기동

```bash
# discovery-server 먼저
./gradlew :discovery-server:bootRun &

# config-server
./gradlew :config-server:bootRun &

# api-gateway
./gradlew :api-gateway:bootRun &

# delivery-service (예시)
./gradlew :delivery-service:bootRun &
```

---

## 5단계: 기동 확인 체크리스트

```
□ Eureka 대시보드 접속 → http://localhost:8761
  └ 각 서비스가 UP 상태로 등록돼 있는지 확인

□ API Gateway 헬스체크
  └ GET http://localhost:8080/actuator/health → {"status":"UP"}

□ Zipkin 대시보드 접속
  └ http://localhost:9411

□ delivery-service 동작 확인
  └ GET http://localhost:8080/api/v1/deliveries
     Headers: X-User-Id: <UUID>, X-User-Role: MASTER
     → 200 OK (빈 목록 반환)
```

---

## 6단계: Kafka 이벤트 테스트 (선택)

```bash
# stock.reserved 이벤트 수동 발행 (배송 생성 트리거)
docker exec -it sparta-kafka \
  kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic stock.reserved

# 아래 JSON 입력 후 Enter
{"orderId":"550e8400-e29b-41d4-a716-446655440000","receiverId":"550e8400-e29b-41d4-a716-446655440001","sourceHubId":"550e8400-e29b-41d4-a716-446655440002","destinationHubId":"550e8400-e29b-41d4-a716-446655440003","deliveryAddress":"서울시 강남구 테헤란로 100"}
```

---

## 자주 발생하는 문제

| 증상 | 원인 | 해결책 |
|------|------|------|
| `Connection refused :8761` | discovery-server 미기동 | 1번 서비스 먼저 기동 |
| `Could not fetch config` | config-server 미기동 | 2번 서비스 먼저 기동 |
| Kafka consumer 연결 실패 | Kafka 컨테이너 미기동 | `docker compose up -d kafka` |
| `DataIntegrityViolationException` | DB 테이블 스키마 충돌 | 서비스 재시작 (create-drop 적용) |
| `401 Unauthorized` | JWT 헤더 누락 | `X-User-Id`, `X-User-Role` 헤더 추가 |
| `403 Forbidden` | 권한 없음 | 역할 확인 (MASTER 사용 시 전체 접근) |

---

## 유용한 URL 모음

| 서비스 | URL |
|--------|-----|
| API Gateway | http://localhost:8080 |
| Eureka 대시보드 | http://localhost:8761 |
| Zipkin 추적 | http://localhost:9411 |
| Kafdrop (설치 시) | http://localhost:9000 |
