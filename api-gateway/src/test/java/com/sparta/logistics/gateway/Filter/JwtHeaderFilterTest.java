package com.sparta.logistics.gateway.Filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.gateway.filter.JwtHeaderFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.time.Instant;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JwtHeaderFilter 단위 테스트
 *
 * 실제 ObjectMapper 사용 (직렬화 로직까지 검증).
 * ReactiveJwtDecoder만 Mock으로 격리.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtHeaderFilter 단위 테스트")
class JwtHeaderFilterTest {

    @Mock
    ReactiveJwtDecoder jwtDecoder;

    // 실제 ObjectMapper 사용: ApiResponse 직렬화 경로도 함께 검증
    ObjectMapper objectMapper = new ObjectMapper();

    JwtHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtHeaderFilter(objectMapper, jwtDecoder);
    }

    // =========================================================
    // 화이트리스트
    // =========================================================

    @Nested
    @DisplayName("화이트리스트 경로")
    class Whitelist {

        @Test
        @DisplayName("로그인 경로는 토큰 없이 통과")
        void login_passesWithoutToken() {
            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchangeFor("/api/v1/auth/login"), chain))
                    .verifyComplete();

            verify(jwtDecoder, never()).decode(any());
            verify(chain).filter(any());
        }

        @Test
        @DisplayName("회원가입 경로는 토큰 없이 통과")
        void signup_passesWithoutToken() {
            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchangeFor("/api/v1/auth/signup"), chain))
                    .verifyComplete();

            verify(jwtDecoder, never()).decode(any());
        }

        @Test
        @DisplayName("Swagger UI 경로는 토큰 없이 통과")
        void swaggerUi_passesWithoutToken() {
            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchangeFor("/swagger-ui/index.html"), chain))
                    .verifyComplete();

            verify(jwtDecoder, never()).decode(any());
        }

        @Test
        @DisplayName("OpenAPI 문서 경로는 토큰 없이 통과")
        void openApiDocs_passesWithoutToken() {
            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchangeFor("/v3/api-docs/swagger-config"), chain))
                    .verifyComplete();

            verify(jwtDecoder, never()).decode(any());
        }
    }

    // =========================================================
    // 토큰 없음 / 형식 오류
    // =========================================================

    @Nested
    @DisplayName("토큰 없음 / 형식 오류")
    class MissingOrMalformedToken {

        @Test
        @DisplayName("Authorization 헤더 없으면 401 TOKEN_NOT_FOUND")
        void noHeader_statusIs401() {
            MockServerWebExchange exchange = exchangeFor("/api/v1/orders");

            StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Bearer 형식이 아닌 토큰이면 401")
        void nonBearerToken_returns401() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/orders")
                            .header("Authorization", "Basic dXNlcjpwYXNz")
                            .build()
            );

            StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // =========================================================
    // 유효한 토큰 → 헤더 주입
    // =========================================================

    @Nested
    @DisplayName("유효한 토큰 - 헤더 주입")
    class ValidToken {

        @Test
        @DisplayName("X-User-Id, X-User-Role 헤더 설정 후 체인 통과")
        void setsUserIdAndRole() {
            String userId = "550e8400-e29b-41d4-a716-446655440000";
            String token  = "valid.jwt.token";
            when(jwtDecoder.decode(token)).thenReturn(Mono.just(buildJwt(userId, "MASTER", null, null)));

            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchangeWithToken("/api/v1/orders", token), chain))
                    .verifyComplete();

            verify(chain).filter(argThat(ex -> {
                HttpHeaders h = ex.getRequest().getHeaders();
                return userId.equals(h.getFirst("X-User-Id"))
                        && "MASTER".equals(h.getFirst("X-User-Role"));
            }));
        }

        @Test
        @DisplayName("hubId 클레임이 있으면 X-User-HubId 헤더도 설정")
        void setsHubIdHeader_whenClaimPresent() {
            String userId = "550e8400-e29b-41d4-a716-446655440000";
            String hubId  = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
            String token  = "valid.hub.token";
            when(jwtDecoder.decode(token)).thenReturn(Mono.just(buildJwt(userId, "HUB_MANAGER", hubId, null)));

            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchangeWithToken("/api/v1/hubs", token), chain))
                    .verifyComplete();

            verify(chain).filter(argThat(ex ->
                    hubId.equals(ex.getRequest().getHeaders().getFirst("X-User-HubId"))
            ));
        }

        @Test
        @DisplayName("companyId 클레임이 있으면 X-User-CompanyId 헤더도 설정")
        void setsCompanyIdHeader_whenClaimPresent() {
            String userId     = "550e8400-e29b-41d4-a716-446655440000";
            String companyId  = "11111111-2222-3333-4444-555555555555";
            String token      = "valid.company.token";
            when(jwtDecoder.decode(token)).thenReturn(Mono.just(buildJwt(userId, "COMPANY_MANAGER", null, companyId)));

            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchangeWithToken("/api/v1/companies", token), chain))
                    .verifyComplete();

            verify(chain).filter(argThat(ex ->
                    companyId.equals(ex.getRequest().getHeaders().getFirst("X-User-CompanyId"))
            ));
        }
    }

    // =========================================================
    // 보안: 헤더 조작 방지
    // =========================================================

    @Nested
    @DisplayName("보안 - 헤더 조작 방지")
    class SecurityHeaderStripping {

        @Test
        @DisplayName("클라이언트가 X-Internal-Call을 보내도 하위 서비스로 전달되지 않음")
        void internalCallHeader_isRemoved() {
            String token = "valid.jwt.token";
            when(jwtDecoder.decode(token)).thenReturn(
                    Mono.just(buildJwt("550e8400-e29b-41d4-a716-446655440000", "MASTER", null, null))
            );

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/orders")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Internal-Call", "true")   // 악의적 주입 시도
                            .build()
            );
            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(argThat(ex ->
                    ex.getRequest().getHeaders().getFirst("X-Internal-Call") == null
            ));
        }

        @Test
        @DisplayName("클라이언트가 X-User-Id를 조작해도 JWT 기반 값으로 덮어씀")
        void userIdHeader_isOverwrittenByJwt() {
            String realUserId = "550e8400-e29b-41d4-a716-446655440000";
            String fakeUserId = "00000000-0000-0000-0000-000000000000";
            String token      = "valid.jwt.token";
            when(jwtDecoder.decode(token)).thenReturn(
                    Mono.just(buildJwt(realUserId, "MASTER", null, null))
            );

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/orders")
                            .header("Authorization", "Bearer " + token)
                            .header("X-User-Id", fakeUserId)   // 조작 시도
                            .build()
            );
            GatewayFilterChain chain = chainThatSucceeds();

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(argThat(ex ->
                    realUserId.equals(ex.getRequest().getHeaders().getFirst("X-User-Id"))
            ));
        }
    }

    // =========================================================
    // 토큰 검증 실패
    // =========================================================

    @Nested
    @DisplayName("토큰 검증 실패")
    class TokenValidationFailure {

        @Test
        @DisplayName("subject가 UUID 형식이 아니면 401 INCORRECT_TOKEN")
        void nonUuidSubject_returns401() {
            String token = "bad.subject.token";
            when(jwtDecoder.decode(token)).thenReturn(
                    Mono.just(buildJwt("not-a-uuid", "MASTER", null, null))
            );

            MockServerWebExchange exchange = exchangeWithToken("/api/v1/orders", token);

            StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("auth 클레임이 없으면 401 INCORRECT_TOKEN")
        void missingAuthClaim_returns401() {
            String token = "no.auth.token";
            when(jwtDecoder.decode(token)).thenReturn(
                    Mono.just(buildJwt("550e8400-e29b-41d4-a716-446655440000", null, null, null))
            );

            MockServerWebExchange exchange = exchangeWithToken("/api/v1/orders", token);

            StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("만료된 토큰이면 401 TOKEN_EXPIRED")
        void expiredToken_returns401() {
            String token = "expired.jwt.token";
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "expired", null);
            when(jwtDecoder.decode(token)).thenReturn(
                    Mono.error(new JwtValidationException("expired", List.of(error)))
            );

            MockServerWebExchange exchange = exchangeWithToken("/api/v1/orders", token);

            StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("서명이 잘못된 토큰(BadJwtException)이면 401 INCORRECT_TOKEN")
        void badJwt_returns401() {
            String token = "tampered.jwt.token";
            when(jwtDecoder.decode(token)).thenReturn(
                    Mono.error(new BadJwtException("bad signature"))
            );

            MockServerWebExchange exchange = exchangeWithToken("/api/v1/orders", token);

            StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("예상치 못한 예외 발생 시 500 INTERNAL_SERVER_ERROR")
        void unexpectedException_returns500() {
            String token = "some.token";
            when(jwtDecoder.decode(token)).thenReturn(
                    Mono.error(new RuntimeException("unexpected db error"))
            );

            MockServerWebExchange exchange = exchangeWithToken("/api/v1/orders", token);

            StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // =========================================================
    // 헬퍼 메서드
    // =========================================================

    private MockServerWebExchange exchangeFor(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }

    private MockServerWebExchange exchangeWithToken(String path, String token) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get(path)
                        .header("Authorization", "Bearer " + token)
                        .build()
        );
    }

    private GatewayFilterChain chainThatSucceeds() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        return chain;
    }

    /**
     * 실제 Jwt 객체 생성 헬퍼.
     * mock(Jwt.class) 사용 시 getSubject()가 default 인터페이스 메서드라
     * 내부에서 getClaimAsString("sub")를 호출해 Mockito 충돌이 발생하므로,
     * Jwt.withTokenValue() 빌더로 실제 객체를 생성한다.
     */
    private Jwt buildJwt(String subject, String auth, String hubId, String companyId) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        if (subject != null)    builder.subject(subject);
        if (auth != null)       builder.claim("auth", auth);
        if (hubId != null)      builder.claim("hubId", hubId);
        if (companyId != null)  builder.claim("companyId", companyId);

        return builder.build();
    }
}
