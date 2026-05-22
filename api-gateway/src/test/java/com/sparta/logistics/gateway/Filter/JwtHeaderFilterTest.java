package com.sparta.logistics.gateway.Filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.gateway.filter.JwtHeaderFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Mockito 정적 메서드 (mock, when, any, never, verify, argThat)
import static org.mockito.Mockito.*;

// AssertJ (assertThat)
import static org.assertj.core.api.Assertions.assertThat;

// StepVerifier (reactor-test)
import reactor.test.StepVerifier;

// Jwt (Spring Security OAuth2)
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

// Spring WebFlux Mock
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Mono;
import java.util.List;


import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class JwtHeaderFilterTest {

    @Mock
    ReactiveJwtDecoder jwtDecoder;
    @Mock
    ObjectMapper objectMapper;

    JwtHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtHeaderFilter(objectMapper, jwtDecoder);
    }

    @Test
    @DisplayName("화이트리스트 경로는 토큰 없이 통과")
    void whiteList_shouldPassWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtDecoder, never()).decode(any());
    }

    @Test
    @DisplayName("토큰 없으면 401 반환")
    void noToken_shouldReturn401() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("유효한 토큰이면 헤더에 userId, role 세팅")
    void validToken_shouldSetUserHeaders() {
        String token = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(jwt.getClaimAsString("auth")).thenReturn("MASTER");
        when(jwtDecoder.decode(token)).thenReturn(Mono.just(jwt));

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(argThat(ex -> {
            HttpHeaders headers = ex.getRequest().getHeaders();
            return "550e8400-e29b-41d4-a716-446655440000".equals(headers.getFirst("X-User-Id"))
                    && "MASTER".equals(headers.getFirst("X-User-Role"));
        }));
    }

    @Test
    @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 에러")
    void expiredToken_shouldReturnExpiredError() throws Exception {
        String token = "expired.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "expired", null);
        JwtValidationException ex = new JwtValidationException("expired", List.of(error));
        when(jwtDecoder.decode(token)).thenReturn(Mono.error(ex));
        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());

        StepVerifier.create(filter.filter(exchange, mock(GatewayFilterChain.class)))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}