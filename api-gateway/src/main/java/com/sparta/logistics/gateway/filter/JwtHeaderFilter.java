package com.sparta.logistics.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.gateway.exception.JwtErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * 게이트웨이 진입 요청에 대해 {@code Authorization: Bearer} 액세스 JWT를 검증하고,
 * 하류 서비스용으로 {@code X-User-Id}, {@code X-User-Role} 헤더를 설정한다.
 */
@Component
@Slf4j
public class JwtHeaderFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper; // 자바 객체 -> json 형태로 변환
    private final ReactiveJwtDecoder jwtDecoder;

    public JwtHeaderFilter(ObjectMapper objectMapper, ReactiveJwtDecoder jwtDecoder){
        this.objectMapper = objectMapper;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static final Set<String> WHITE_LIST = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/refresh"
    );

    private boolean isWhiteList(String path){
        return WHITE_LIST.contains(path) ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();


        //whitelist 경로는 필터를 통과시키지 않음
        if(isWhiteList(path)) return chain.filter(exchange);

        // 토큰만 반환
        String token = extractToken(request);

        // 토큰이 null인 경우 에러
        if(token == null) {
            log.warn("Authorization 헤더가 없거나 Bearer 토큰이 아님");
            return onError(exchange, JwtErrorCode.TOKEN_NOT_FOUND);
        }


        return jwtDecoder.decode(token)
                .flatMap(jwt -> {

                    String userId = jwt.getSubject();

                    if (!StringUtils.hasText(userId)) {
                        log.warn("JWT subject(userId) 가 비어 있음");
                        return onError(exchange, JwtErrorCode.INCORRECT_TOKEN);
                    }
                    String normalUserId = userId.trim(); // 공백 제거
                    try {
                        UUID.fromString(normalUserId); // string -> UUID 변환 후 검증
                    } catch (IllegalArgumentException ex) {
                        log.error("JWT subject 가 UUID 형식이 아님");
                        return onError(exchange, JwtErrorCode.INCORRECT_TOKEN);
                    }

                    // 권한 추출
                    String finalRole = jwt.getClaimAsString("auth");

                    // 권한이 null, 빈값, 공백 문자열이면 에러
                    if (!StringUtils.hasText(finalRole)) {
                        log.warn("auth 클레임이 없거나 비어 있음");
                        return onError(exchange, JwtErrorCode.INCORRECT_TOKEN);
                    }

                    String hubId = jwt.getClaimAsString("hubId");
                    String companyId = jwt.getClaimAsString("companyId");

                    ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
                    builder.headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                        headers.remove("X-User-HubId");
                        headers.remove("X-User-CompanyId");
                        headers.set("X-User-Id", normalUserId);
                        headers.set("X-User-Role", finalRole);

                        if (StringUtils.hasText(hubId))
                            headers.set("X-User-HubId",hubId.trim());

                        if (StringUtils.hasText(companyId))
                            headers.set("X-User-CompanyId",companyId.trim());

                    });
                    return chain.filter(exchange.mutate().request(builder.build()).build());
                })
                .onErrorResume(e -> {
                    if (e instanceof JwtValidationException jwtEx) {
                        // 만료 여부는 에러 코드로 판별
                        boolean isExpired = jwtEx.getErrors().stream()
                                .anyMatch(err -> err.getErrorCode()
                                        .equals(OAuth2ErrorCodes.INVALID_TOKEN)
                                        && err.getDescription().contains("expired"));

                        if (isExpired) {
                            log.warn("Gateway Auth Warning: 토큰 만료됨");
                            return onError(exchange, JwtErrorCode.TOKEN_EXPIRED);
                        }
                        log.warn("Gateway Auth Error: 토큰 검증 실패 - {}", e.getMessage());
                        return onError(exchange, JwtErrorCode.INCORRECT_TOKEN);
                    }
                    if (e instanceof BadJwtException) { // 토큰 형식 망가짐
                        log.warn("Gateway Auth Error: 유효하지 않은 토큰 - {}", e.getMessage());
                        return onError(exchange, JwtErrorCode.INCORRECT_TOKEN);
                    } // 그 외
                    log.error("Gateway 내부 인증 시스템 심각한 예외 발생", e);
                    return onError(exchange, JwtErrorCode.INTERNAL_SERVER_ERROR);
                });
    }


    // 토큰 추출
    private String extractToken(ServerHttpRequest request){
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }

    //비동기 방식으로 응답 에러 처리
    private Mono<Void> onError(ServerWebExchange exchange, JwtErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(errorCode.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON); // 응답헤더 json으로 지정

        return Mono.defer(() -> {
            try {
                ApiResponse<Void> apiResponse = ApiResponse.error(errorCode); // 공통 에러 객체 생성

                byte[] jsonBytes = objectMapper.writeValueAsBytes(apiResponse);
                DataBuffer buffer = response.bufferFactory().wrap(jsonBytes);

                return response.writeWith(Mono.just(buffer));
            } catch (Exception e) {
                log.warn("JSON 변환 중 예외 발생", e);
                return response.setComplete();
            }
        });
    }
}