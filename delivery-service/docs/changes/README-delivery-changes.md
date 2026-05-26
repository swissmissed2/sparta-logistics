# Delivery Service — 코드 변경 사항

  
- **커밋 해시** `c0b871f`
- 배송 생성 API 구현으로 인한 파일별 변경 내역입니다.

---

## 수정된 파일

### 1. `build.gradle`

**위치**: `delivery-service/build.gradle`

**커밋 6a72aca, 이슈 #150 참고** (`카프카 이벤트 발행`을 위한 설정)
```groovy
dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
    implementation 'org.springframework.kafka:spring-kafka'          // ← 추가
    testImplementation 'org.springframework.kafka:spring-kafka-test' // ← 추가
    runtimeOnly 'org.postgresql:postgresql'
}
```

---

### 2. `application.yaml`

**위치**: `delivery-service/src/main/resources/application.yaml`

**Before**: Kafka 설정 없음  
**After**: spring 블록 내에 추가
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: delivery-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

> consumer/producer 모두 String 직렬화 사용. 이벤트 핸들러 내부에서 ObjectMapper로 수동 역직렬화.

---

### 3. `entity/DeliveryEntity.java`

**위치**: `delivery-service/src/main/java/com/sparta/logistics/delivery/entity/DeliveryEntity.java`

**Before** (생성자, line 66):
```java
// TODO: 생성
public DeliveryEntity(UUID orderId, String deliveryAddress, String receiverSlackId) {
    this.orderId = orderId;
    this.deliveryAddress = deliveryAddress;
    this.receiverSlackId = receiverSlackId;
    this.status = DeliveryStatus.CREATED;
}
```
문제: `sourceHubId`, `destinationHubId` 미설정 → DB `nullable=false` 제약 위반 발생

**After**:
```java
public DeliveryEntity(UUID orderId, UUID receiverId,
                      UUID sourceHubId, UUID destinationHubId,
                      String deliveryAddress, String receiverSlackId) {
    this.orderId = orderId;
    this.receiverId = receiverId;
    this.sourceHubId = sourceHubId;
    this.destinationHubId = destinationHubId;
    this.deliveryAddress = deliveryAddress;
    this.receiverSlackId = receiverSlackId;
    this.status = DeliveryStatus.CREATED;
}

// ai.deadline.calculated 이벤트 수신 시 호출
public void updateFinalDispatchDeadline(LocalDateTime deadline) {
    this.finalDispatchDeadlineAt = deadline;
}
```

---

### 4. `service/DeliveryService.java`

**위치**: `delivery-service/src/main/java/com/sparta/logistics/delivery/service/DeliveryService.java`

**Before** (createDelivery, line 86-94):
```java
public DeliveryDetailResponse createDelivery(DeliveryCreateRequest request, UUID userId) {
    // TODO: 주문-사용자 관계 검증 위치 표시
    // TODO: Delivery 생성 위치 표시
    // TODO: 예외 처리 등 로직 보강
    return DeliveryDetailResponse.from(deliveryRepository.save());  // ← 컴파일 에러, 인자 없음
}
```

**After** (Kafka 이벤트 기반 생성 메서드 추가):
```java
// DeliveryEventHandler에서 Feign 호출 후 진입 — 트랜잭션 범위 최소화
@Transactional
public void createDelivery(StockReservedEventDto event, String slackId) {
    DeliveryEntity entity = new DeliveryEntity(
        event.orderId(),
        event.receiverId(),
        event.sourceHubId(),
        event.destinationHubId(),
        event.deliveryAddress(),
        slackId
    );
    deliveryRepository.save(entity);
}

// ai.deadline.calculated 이벤트 수신 시 호출
@Transactional
public void updateFinalDispatchDeadline(UUID deliveryId, LocalDateTime deadline) {
    DeliveryEntity entity = deliveryRepository.findById(deliveryId)
        .orElseThrow(() -> new IllegalArgumentException("배송 없음: " + deliveryId));
    entity.updateFinalDispatchDeadline(deadline);
}
```

---

### 5. `infrastructure/event/DeliveryEventHandler.java`

**위치**: `delivery-service/src/main/java/com/sparta/logistics/delivery/infrastructure/event/DeliveryEventHandler.java`

**Before**:
```java
public class DeliveryEventHandler {
}
```

**After**:
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventHandler {

    private final DeliveryService deliveryService;
    private final DeliveryEventPublisher eventPublisher;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    // stock.reserved 이벤트 소비 → 배송 생성
    @KafkaListener(topics = "stock.reserved", groupId = "delivery-service")
    public void handleStockReserved(String message) {
        StockReservedEventDto event;
        try {
            event = objectMapper.readValue(message, StockReservedEventDto.class);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] stock.reserved 역직렬화 실패: {}", message, e);
            return;
        }

        // 허브 ID 검증
        if (event.sourceHubId() == null || event.destinationHubId() == null) {
            log.warn("[Kafka] stock.reserved 허브 ID null — orderId={}", event.orderId());
            eventPublisher.publishCreationFailed(event.orderId(), "INVALID_HUB_ID");
            return;
        }

        // user-service Feign 호출 (트랜잭션 밖)
        String slackId;
        try {
            slackId = userServiceClient.getUser(event.receiverId()).data().slackId();
        } catch (Exception e) {
            log.warn("[Kafka] user-service 호출 실패 — orderId={}", event.orderId(), e);
            eventPublisher.publishCreationFailed(event.orderId(), "USER_SERVICE_UNAVAILABLE");
            return;
        }

        if (slackId == null) {
            log.warn("[Kafka] slackId 없음 — orderId={}", event.orderId());
            eventPublisher.publishCreationFailed(event.orderId(), "SLACK_ID_NOT_FOUND");
            return;
        }

        deliveryService.createDelivery(event, slackId);
        log.info("[Kafka] 배송 생성 완료 — orderId={}", event.orderId());
    }

    // ai.deadline.calculated 이벤트 소비 → finalDispatchDeadlineAt 업데이트
    @KafkaListener(topics = "ai.deadline.calculated", groupId = "delivery-service")
    public void handleAiDeadlineCalculated(String message) {
        AiDeadlineCalculatedEvent event;
        try {
            event = objectMapper.readValue(message, AiDeadlineCalculatedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] ai.deadline.calculated 역직렬화 실패: {}", message, e);
            return;
        }
        deliveryService.updateFinalDispatchDeadline(event.deliveryId(), event.finalDispatchDeadlineAt());
        log.info("[Kafka] AI 발송 시한 업데이트 — deliveryId={}", event.deliveryId());
    }
}
```

---

### 6. `dto/DeliveryDetailResponse.java`
> 추후 DTO 구조 분리 (데이터 보호), Kafka 용 DTO 통합 구조를 적용할 필요성이 있음.

**위치**: `delivery-service/src/main/java/com/sparta/logistics/delivery/dto/DeliveryDetailResponse.java`

**Before** (3개 필드):
```java
public record DeliveryDetailResponse(
    UUID deliveryId,
    DeliveryStatus status,
    String deliveryAddress
) {
    public static DeliveryDetailResponse from(DeliveryEntity deliveryEntity) {
        return new DeliveryDetailResponse(
            deliveryEntity.getId(),
            deliveryEntity.getStatus(),
            deliveryEntity.getDeliveryAddress()
        );
    }
}
```

**After** (전체 필드 노출):
```java
public record DeliveryDetailResponse(
    UUID deliveryId,
    UUID orderId,
    DeliveryStatus status,
    UUID sourceHubId,
    UUID destinationHubId,
    UUID currentHubId,
    String deliveryAddress,
    UUID receiverId,
    String receiverSlackId,
    UUID deliveryManagerId,
    LocalDateTime finalDispatchDeadlineAt,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {
    public static DeliveryDetailResponse from(DeliveryEntity e) {
        return new DeliveryDetailResponse(
            e.getId(), e.getOrderId(), e.getStatus(),
            e.getSourceHubId(), e.getDestinationHubId(), e.getCurrentHubId(),
            e.getDeliveryAddress(), e.getReceiverId(), e.getReceiverSlackId(),
            e.getDeliveryManagerId(), e.getFinalDispatchDeadlineAt(),
            e.getStartedAt(), e.getCompletedAt()
        );
    }
}
```

---

### 7. `client/UserServiceClient.java`

**위치**: `delivery-service/src/main/java/com/sparta/logistics/delivery/client/UserServiceClient.java`

**Before**:
```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/users/data")
    String getUserData();
}
```

**After**:
```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    // 엔드포인트 재확인 요망
    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable UUID userId);
}
```

---

## 신규 생성 파일

### [수정중] `dto/event/StockReservedEventDto.java`
```java
// stock.reserved 이벤트 수신용 DTO
// #158 kafka 전용 공통 DTO 참고하여 수정 필요
public record StockReservedEventDto(
    UUID orderId,
    UUID receiverId,
    UUID sourceHubId,
    UUID destinationHubId,
    String deliveryAddress
) {}
```

### [수정중] `dto/event/DeliveryCreationFailedEvent.java`
```java
// delivery.creation.failed 이벤트 발행용 DTO
public record DeliveryCreationFailedEvent(
    UUID orderId,
    String reason
) {}
```

### [수정중] `dto/event/AiDeadlineCalculatedEvent.java`
```java
// ai.deadline.calculated 이벤트 수신용 DTO
public record AiDeadlineCalculatedEvent(
    UUID deliveryId,
    LocalDateTime finalDispatchDeadlineAt
) {}
```

### `infrastructure/event/DeliveryEventPublisher.java`
```java
// delivery.creation.failed 이벤트 발행 담당
// 로직 보강 중...
@Component
@RequiredArgsConstructor
public class DeliveryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishCreationFailed(UUID orderId, String reason) {
        try {
            String message = objectMapper.writeValueAsString(
                new DeliveryCreationFailedEvent(orderId, reason)
            );
            kafkaTemplate.send("delivery.creation.failed", message);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] delivery.creation.failed 발행 실패 orderId={}", orderId, e);
        }
    }
}
```

### [수정중] `client/response/UserResponse.java`
```java
// user-service 응답 DTO — 재점검 요망
public record UserResponse(
    UUID userId,
    String slackId
) {}
```

---

## 변경이 없는 파일 (참고)

| 파일 | 이유 |
|------|------|
| `DeliveryController.java` | POST 엔드포인트는 Kafka로 대체됨 (기존 엔드포인트 TODO 상태 유지) |
| `DeliveryRepository.java` | 기존 save() 메서드 그대로 사용 |
| `DeliverySearchCond.java` | 조회 기능이므로 생성 로직과 무관 |
| `DeliveryListResponse.java` | 변경 없음 |

---

## 차후 수정 방향
- SA 문서에 맞게 폴더 구조 수정 필요 (delivery deliveryRoute deliveryManager로 구분)
- 배송 생성 시 알람 기능 필요한지 확인 및 구현
- `UserResponse` DTO 구조 재점검
- `DeliveryCreationFailedEvent` 이벤트 발행 로직 재점검
- `AI 최종 발송 시한` DTO 재점검
- `StockReservedEventDto` 재점검
- `DeliveryDetailResponse` DTO 구조 분리 검토
- Kafka용 통합 DTO 반영