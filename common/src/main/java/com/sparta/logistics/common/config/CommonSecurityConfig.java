package com.sparta.logistics.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 나중에 각 서비스에서 @PreAuthorize를 쓰기 위해 공통으로 켜둡니다!
public class CommonSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (JWT 사용하므로 불필요)
                .csrf(csrf -> csrf.disable())
                // 2. 기본 로그인 폼 및 HTTP Basic 인증 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 3. 세션을 사용하지 않음 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. 공통 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Swagger 및 Actuator는 인증 없이 통과
                        .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // TODO: 개발 편의용 설정 (배포 전에는 인증 필요하도록 변경 필요)
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}