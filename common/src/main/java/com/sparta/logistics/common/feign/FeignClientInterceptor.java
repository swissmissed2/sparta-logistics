package com.sparta.logistics.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor{

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_HUB_ID_HEADER = "X-User-HubId";
    private static final String USER_COMPANY_ID_HEADER = "X-User-CompanyId";
    public static final String INTERNAL_CALL_HEADER = "X-Internal-Call";
    public static final String INTERNAL_CALL_VALUE = "true";

    @Override
    public void apply(RequestTemplate template){
        // 모든 내부 Feign 호출에 내부 서비스 식별 헤더를 추가
        template.header(INTERNAL_CALL_HEADER, INTERNAL_CALL_VALUE);

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null){
            HttpServletRequest request = attributes.getRequest();
            String userId = request.getHeader(USER_ID_HEADER);
            String userRole = request.getHeader(USER_ROLE_HEADER);
            String hubId = request.getHeader(USER_HUB_ID_HEADER);
            String companyId = request.getHeader(USER_COMPANY_ID_HEADER);

            if (StringUtils.hasText(userId)) template.header(USER_ID_HEADER, userId);
            if (StringUtils.hasText(userRole)) template.header(USER_ROLE_HEADER, userRole);
            if (StringUtils.hasText(hubId)) template.header(USER_HUB_ID_HEADER, hubId);
            if (StringUtils.hasText(companyId)) template.header(USER_COMPANY_ID_HEADER, companyId);
        }
    }
}
