package com.sparta.logistics.common.filter;

import com.sparta.logistics.common.constants.SystemConstants;
import com.sparta.logistics.common.feign.FeignClientInterceptor;
import com.sparta.logistics.common.security.GatewayAuthEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final GatewayAuthEntryPoint gatewayAuthEntryPoint;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {


        // 내부 서비스 호출인 경우 SYSTEM 권한으로 SecurityContext 세팅 후 통과
        String internalCall = request.getHeader(FeignClientInterceptor.INTERNAL_CALL_HEADER);
        if (FeignClientInterceptor.INTERNAL_CALL_VALUE.equals(internalCall)) {
            UsernamePasswordAuthenticationToken systemAuth =
                    new UsernamePasswordAuthenticationToken(
                            SystemConstants.SYSTEM_UUID.toString(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))
                    );
            SecurityContextHolder.getContext().setAuthentication(systemAuth);
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String hubId = request.getHeader("X-User-HubId");
        String companyId = request.getHeader("X-User-CompanyId");


        if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {

            try {
                UUID.fromString(userId.trim());
            } catch (IllegalArgumentException e) {
                log.warn("GatewayAuthFilter: X-User-Id가 UUID 형식이 아님 - 위조된 헤더 가능성 있음");
                gatewayAuthEntryPoint.commence(request, response, new BadCredentialsException("위조된 헤더"));
                return;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim())); // Spring Security의 전통적인 권한 포맷 맞추기 위함.

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            Map<String, String> customDetails = new HashMap<>();
            if (StringUtils.hasText(hubId)) customDetails.put("hubId", hubId);
            if (StringUtils.hasText(companyId)) customDetails.put("companyId", companyId);

            authentication.setDetails(customDetails);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
