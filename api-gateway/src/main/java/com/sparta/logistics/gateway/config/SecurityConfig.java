package com.sparta.logistics.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // 안써줘도 자동 생성되지만 가독성을 위해 표기하는 걸 추천
@EnableMethodSecurity // @PreAuthorize 사용 가능
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // JWT는 세션/쿠키 안 써서 불필요
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // JWT 방식이라 로그인 폼 불필요
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // JWT 방식이라 Basic 인증 불필요
                .logout(ServerHttpSecurity.LogoutSpec::disable) // JWT는 서버 측 로그아웃 의미 없음
                .authorizeExchange(exchange -> exchange
                        .anyExchange().permitAll()
                )
                .build();
    }

}

