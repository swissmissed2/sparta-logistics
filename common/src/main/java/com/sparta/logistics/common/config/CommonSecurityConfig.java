package com.sparta.logistics.common.config;

import com.sparta.logistics.common.filter.GatewayAuthFilter;
import com.sparta.logistics.common.security.GatewayAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 나중에 각 서비스에서 @PreAuthorize를 쓰기 위해 공통으로 켜둡니다!
@RequiredArgsConstructor
public class CommonSecurityConfig {

    private final GatewayAuthEntryPoint gatewayAuthEntryPoint;
    private final GatewayAuthFilter gatewayAuthFilter;

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
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/refresh"
                        ).permitAll()
                        // TODO: 개발 편의용 설정 (배포 전에는 인증 필요하도록 변경 필요)
                        .anyRequest().permitAll()
                )
                // 5. 인증 실패 시 GatewayAuthEntryPoint로 처리
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(gatewayAuthEntryPoint)
                )
                // GatewayAuthFilter를 AuthorizationFilter 이전에 등록
                // → SecurityContext에 인증 정보를 먼저 세팅한 뒤 권한 체크가 이루어짐
                .addFilterBefore(gatewayAuthFilter, AuthorizationFilter.class);
        return http.build();
    }

    // GatewayAuthFilter는 Security FilterChain에만 등록되어야 함.
    // @Component에 의해 서블릿 필터로도 자동 등록되는 것을 방지함.
    @Bean
    public FilterRegistrationBean<GatewayAuthFilter> gatewayAuthFilterRegistration(GatewayAuthFilter filter) {
        FilterRegistrationBean<GatewayAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}