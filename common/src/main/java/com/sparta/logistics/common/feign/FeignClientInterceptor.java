package com.sparta.logistics.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor{

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_HUB_ID_HEADER = "X-User-HubId";
    private static final String USER_COMPANY_ID_HEADER = "X-User-CompanyId";

    @Override
    public void apply(RequestTemplate template){
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null){
            HttpServletRequest request = attributes.getRequest();
            String userId = request.getHeader(USER_ID_HEADER);
            String userRole = request.getHeader(USER_ROLE_HEADER);
            String hubId = request.getHeader(USER_HUB_ID_HEADER);
            String companyId = request.getHeader(USER_COMPANY_ID_HEADER);

            if (userId != null) template.header(USER_ID_HEADER, userId);
            if (userRole != null) template.header(USER_ROLE_HEADER, userRole);
            if (hubId != null) template.header(USER_HUB_ID_HEADER, hubId);
            if (companyId != null) template.header(USER_COMPANY_ID_HEADER, companyId);
        }
    }
}
