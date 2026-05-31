# 인증·인가 흐름 가이드

---

## 1. 전체 흐름

```
클라이언트
  │  Authorization: Bearer <JWT>
  ▼
API Gateway (:8080)
  │  1. JWT 서명 검증 (HS256, secret-key)
  │  2. Claims 파싱 → X-User-* 헤더 주입
  │  3. 원본 Authorization 헤더 제거
  ▼
각 마이크로서비스
  │  X-User-Id, X-User-Role, X-User-HubId, X-User-CompanyId 헤더만 수신
  │  JWT 재검증 없음 — Gateway 신뢰 모델
  ▼
서비스 내 권한 검사
  └── EX) DeliveryPermissionChecker (delivery-service)
  └── 각 서비스 자체 권한 로직
```

---

## 2. JWT 구조

JWT는 `user-service`에서 발급합니다.

```
Header: { "alg": "HS256", "typ": "JWT" }
Payload: {
  "sub":       "사용자 UUID",
  "role":      "MASTER | HUB_MANAGER | DELIVERY_MANAGER | COMPANY_MANAGER",
  "hubId":     "UUID (HUB_MANAGER인 경우)",
  "companyId": "UUID (COMPANY_MANAGER인 경우)",
  "iat": 발급 시각,
  "exp": 만료 시각
}
```

---

## 3. API Gateway가 주입하는 헤더

| 헤더 | 타입 | 항상 존재 | 설명 |
|------|------|---------|------|
| `X-User-Id` | UUID | ✅ | 사용자 고유 ID |
| `X-User-Role` | String | ✅ | 역할 (아래 목록 참고) |
| `X-User-HubId` | UUID | ❌ | HUB_MANAGER인 경우에만 존재 |
| `X-User-CompanyId` | UUID | ❌ | COMPANY_MANAGER인 경우에만 존재 |

### 마이크로서비스에서 수신 방법

```java
@GetMapping("/{id}")
public ResponseEntity<?> get(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id")                       UUID userId,
        @RequestHeader("X-User-Role")                     String role,
        @RequestHeader(value = "X-User-HubId",     required = false) UUID hubId,
        @RequestHeader(value = "X-User-CompanyId", required = false) UUID companyId
) { ... }
```

---

## 4. 역할(Role) 목록 및 권한 범위

| 역할 | 설명 | 특수 헤더 |
|------|------|---------|
| `MASTER` | 관리자 — 모든 리소스 접근 가능 | 없음 |
| `HUB_MANAGER` | 허브 담당자 — 자기 허브 소속 리소스만 접근 | `X-User-HubId` (담당 허브 UUID) |
| `DELIVERY_MANAGER` | 배송담당자 — 자신에게 배정된 배송·경로만 접근 | 없음 (userId로 식별) |
| `COMPANY_MANAGER` | 업체 담당자 — 자기 업체 주문·배송만 조회 가능 | `X-User-CompanyId` (담당 업체 UUID) |

---

## 5. API Gateway 라우팅 규칙

```yaml
routes:
  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/api/v1/auth/**, /api/v1/users/**

  - id: hub-service
    uri: lb://hub-service
    predicates:
      - Path=/api/v1/hubs/**, /api/v1/hub-routes/**

  - id: company-service
    uri: lb://company-service
    predicates:
      - Path=/api/v1/companies/**

  - id: product-service
    uri: lb://product-service
    predicates:
      - Path=/api/v1/products/**

  - id: order-service
    uri: lb://order-service
    predicates:
      - Path=/api/v1/orders/**

  - id: delivery-service
    uri: lb://delivery-service
    predicates:
      - Path=/api/v1/deliveries/**, /api/v1/delivery-managers/**

  - id: slack-service
    uri: lb://slack-service
    predicates:
      - Path=/api/v1/slack-messages/**, /api/v1/ai-logs/**
```

> `lb://` 프리픽스 — Eureka 로드밸런서를 통해 인스턴스 선택

---

## 6. 인증이 불필요한 경로

```
POST /api/v1/auth/signup   # 회원가입
POST /api/v1/auth/login    # 로그인 (JWT 발급)
```

이 두 경로는 Gateway 필터에서 JWT 검증을 건너뜁니다.

---

## 7. 보안 고려사항

| 항목 | 현재 상태 | 개선 방향                                            |
|------|---------|--------------------------------------------------|
| JWT 서명 알고리즘 | HS256 | 운영 환경 RSA(RS256) 전환 필요                           |
| Refresh Token 저장 | Redis | 현재 구현 완료                                         |
| 서비스 간 내부 호출 인증 | 없음 (Gateway 신뢰 모델) | 운영 환경 mTLS 또는 내부 API Key 추가 고려할지 협의 필요           |