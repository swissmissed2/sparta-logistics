package com.sparta.logistics.common.config;

import com.sparta.logistics.common.constants.SystemConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Auditing을 위한 등록자/수정자(Auditor) 자동 추적 설정 클래스
 */
@EnableJpaAuditing
@Configuration
public class AuditorAwareConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader("X-User-Id");

                if (StringUtils.hasText(userId)) {
                    try {
                        return Optional.of(UUID.fromString(userId));
                    } catch (IllegalArgumentException e) {
                        return Optional.of(SystemConstants.SYSTEM_UUID);
                    }
                }
            }
            return Optional.of(SystemConstants.SYSTEM_UUID);
        };
    }
}