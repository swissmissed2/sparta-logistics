package com.sparta.logistics.company.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Company Service 보안 설정
 * 1. Gateway에서 이미 JWT 검증 완료 → 각 서비스는 X-User-Id, X-User-Role 헤더 기반 인증 정보 사용
 * 2. Spring Security는 CSRF 비활성화 + Stateless 세션 정책 적용
 * 3. 실제 권한 검증은 CompanyService 비즈니스 로직에서 처리
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 내부 서비스 통신용 exists 엔드포인트는 인증 불필요
                        .requestMatchers("/api/v1/companies/*/exists").permitAll()
                        // Swagger
                        .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                        //Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // TODO: 개발 편의용 설정 (배포 전 authenticated()로 변경 필수)
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
