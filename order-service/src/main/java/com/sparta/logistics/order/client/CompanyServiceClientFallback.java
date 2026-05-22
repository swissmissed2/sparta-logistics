package com.sparta.logistics.order.client;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.order.client.response.CompanyResponse;
import com.sparta.logistics.order.exception.OrderErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class CompanyServiceClientFallback implements CompanyServiceClient {

    private String warnMessage(UUID companyId) {
        return "[CompanyServiceClient Fallback] Company Service 응답 없음. companyId=" + companyId;
    };

    @Override
    public void checkCompanyExists(UUID companyId) {
        log.warn(warnMessage(companyId));
        throw new BusinessException(OrderErrorCode.COMPANY_SERVICE_UNAVAILABLE);
    }

    @Override
    public ApiResponse<CompanyResponse> getCompany(UUID companyId) {
        log.warn(warnMessage(companyId));
        return ApiResponse.ok(null);
    }
}
