package com.sparta.logistics.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Gateway 통합 테스트
 *
 * - 실제 Spring Context 구동 (RANDOM_PORT)
 * - WireMock: 하위 서비스(user-service 등) 대역
 * - application-test.yml: Config Server, Eureka, Tracing 비활성화
 *   + 라우트를 WireMock URL로 오버라이드
 * - 실제 JWT 생성 (NimbusJwtEncoder) → 실제 JwtDecoder로 검증
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.import=",          // Config Server 연결 차단
                "spring.cloud.config.enabled=false",
                "management.tracing.enabled=false"
        }
)
@ActiveProfiles("test")
@DisplayName("Gateway 통합 테스트")
class GatewayIntegrationTest {

    // application-test.yml 의 jwt.secret-key 와 동일한 값
    private static final String TEST_SECRET = "aGVsbG9Xb3JsZFNlY3JldEtleUZvckpXVEF1dGgxMjM0NTY=";

    // ── WireMock 서버 (하위 서비스 대역) ──────────────────────────────────
    static WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());

    /**
     * @DynamicPropertySource: Spring Context 시작 전에 호출되므로
     * WireMock 포트를 application-test.yml 의 ${downstream.url} 에 주입할 수 있음.
     */
    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        wireMock.start();
        registry.add("downstream.url", () -> "http://localhost:" + wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    // ── WebTestClient ─────────────────────────────────────────────────────
    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    @BeforeEach
    void setUpClient() {
        webTestClient = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // =========================================================
    // 화이트리스트 - 토큰 없이 하위 서비스까지 도달
    // =========================================================

    @Nested
    @DisplayName("화이트리스트 경로")
    class Whitelist {

        @Test
        @DisplayName("로그인 요청은 토큰 없이 하위 서비스로 라우팅됨")
        void login_routesToDownstreamWithoutToken() {
            wireMock.stubFor(post(urlEqualTo("/api/v1/auth/login"))
                    .willReturn(aResponse().withStatus(200).withBody("{\"token\":\"abc\"}")));

            webTestClient.post().uri("/api/v1/auth/login")
                    .exchange()
                    .expectStatus().isOk();

            wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/login")));
        }

        @Test
        @DisplayName("회원가입 요청은 토큰 없이 하위 서비스로 라우팅됨")
        void signup_routesToDownstreamWithoutToken() {
            wireMock.stubFor(post(urlEqualTo("/api/v1/auth/signup"))
                    .willReturn(aResponse().withStatus(201)));

            webTestClient.post().uri("/api/v1/auth/signup")
                    .exchange()
                    .expectStatus().isCreated();
        }
    }

    // =========================================================
    // 인증 실패 → 401
    // =========================================================

    @Nested
    @DisplayName("인증 실패 - 401 반환")
    class AuthFailure {

        @Test
        @DisplayName("토큰 없이 보호된 경로 요청 시 401")
        void noToken_returns401() {
            webTestClient.get().uri("/api/v1/users/me")
                    .exchange()
                    .expectStatus().isUnauthorized();

            // 하위 서비스에 요청이 도달하지 않아야 함
            wireMock.verify(0, getRequestedFor(anyUrl()));
        }

        @Test
        @DisplayName("만료된 토큰으로 요청 시 401")
        void expiredToken_returns401() {
            // issuedAt도 과거로 설정해야 expiresAt > issuedAt 조건 통과
            String token = generateToken(UUID.randomUUID().toString(), "MASTER",
                    Instant.now().minusSeconds(120),   // issuedAt
                    Instant.now().minusSeconds(60));   // expiresAt (이미 만료)

            webTestClient.get().uri("/api/v1/users/me")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectStatus().isUnauthorized();

            wireMock.verify(0, getRequestedFor(anyUrl()));
        }

        @Test
        @DisplayName("서명이 변조된 토큰으로 요청 시 401")
        void tamperedToken_returns401() {
            webTestClient.get().uri("/api/v1/users/me")
                    .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.invalid.signature")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // =========================================================
    // 인증 성공 → 헤더 전달 검증
    // =========================================================

    @Nested
    @DisplayName("인증 성공 - 헤더 전달")
    class AuthSuccess {

        @Test
        @DisplayName("유효한 토큰이면 X-User-Id, X-User-Role 이 하위 서비스에 전달됨")
        void validToken_forwardsUserHeaders() {
            String userId = UUID.randomUUID().toString();
            String token  = generateToken(userId, "MASTER", Instant.now().plusSeconds(3600));

            wireMock.stubFor(get(urlEqualTo("/api/v1/users/me"))
                    .willReturn(aResponse().withStatus(200).withBody("{}")));

            webTestClient.get().uri("/api/v1/users/me")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk();

            // WireMock 으로 실제 수신 헤더 검증
            wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/users/me"))
                    .withHeader("X-User-Id",   equalTo(userId))
                    .withHeader("X-User-Role", equalTo("MASTER")));
        }

        @Test
        @DisplayName("X-Internal-Call 헤더는 하위 서비스에 전달되지 않음")
        void internalCallHeader_isStripped() {
            String token = generateToken(UUID.randomUUID().toString(), "MASTER",
                    Instant.now().plusSeconds(3600));

            wireMock.stubFor(get(urlEqualTo("/api/v1/users/me"))
                    .willReturn(aResponse().withStatus(200)));

            webTestClient.get().uri("/api/v1/users/me")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Internal-Call", "true")   // 악의적 주입 시도
                    .exchange()
                    .expectStatus().isOk();

            // 하위 서비스에 X-Internal-Call 이 없어야 함
            wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/users/me"))
                    .withoutHeader("X-Internal-Call"));
        }

        @Test
        @DisplayName("클라이언트가 X-User-Id 를 조작해도 JWT 기반으로 덮어씌워짐")
        void userIdHeader_isOverwrittenByJwt() {
            String realUserId = UUID.randomUUID().toString();
            String fakeUserId = "00000000-0000-0000-0000-000000000000";
            String token      = generateToken(realUserId, "MASTER", Instant.now().plusSeconds(3600));

            wireMock.stubFor(get(urlEqualTo("/api/v1/users/me"))
                    .willReturn(aResponse().withStatus(200)));

            webTestClient.get().uri("/api/v1/users/me")
                    .header("Authorization", "Bearer " + token)
                    .header("X-User-Id", fakeUserId)   // 조작 시도
                    .exchange()
                    .expectStatus().isOk();

            wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/users/me"))
                    .withHeader("X-User-Id", equalTo(realUserId)));
        }
    }

    // =========================================================
    // 라우팅 규칙
    // =========================================================

    @Nested
    @DisplayName("라우팅 규칙")
    class Routing {

        @Test
        @DisplayName("/api/v1/products/internal/** 경로는 인증 후에도 404 로 차단")
        void internalProductPath_isBlocked() {
            // JwtHeaderFilter가 HIGHEST_PRECEDENCE로 라우팅보다 먼저 실행되므로
            // 토큰 없이 보내면 401이 먼저 나온다. 유효한 토큰으로 JWT를 통과시킨 뒤
            // SetStatus=404 필터가 동작하는지 검증한다.
            String token = generateToken(UUID.randomUUID().toString(), "MASTER",
                    Instant.now().plusSeconds(3600));

            webTestClient.get().uri("/api/v1/products/internal/stock")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectStatus().isNotFound();

            // 하위 서비스까지 도달하지 않아야 함
            wireMock.verify(0, getRequestedFor(anyUrl()));
        }
    }

    // =========================================================
    // JWT 생성 헬퍼
    // =========================================================

    /**
     * application-test.yml 의 jwt.secret-key 와 동일한 키로 테스트용 JWT 생성.
     * NimbusJwtEncoder 는 spring-boot-starter-oauth2-resource-server 에 포함.
     */
    private String generateToken(String userId, String role, Instant expiresAt) {
        return generateToken(userId, role, Instant.now(), expiresAt);
    }

    private String generateToken(String userId, String role, Instant issuedAt, Instant expiresAt) {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("auth", role)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
