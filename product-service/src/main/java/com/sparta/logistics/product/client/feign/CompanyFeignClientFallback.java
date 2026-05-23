package com.sparta.logistics.product.client.feign;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.product.client.model.CompanyClientResponse;
import com.sparta.logistics.product.exception.ProductErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class CompanyFeignClientFallback implements CompanyFeignClient {

    @Override
    public void checkCompanyExists(UUID companyId) {
        log.warn("[CompanyFeignClient Fallback] Company Service 응답 없음. companyId={}", companyId);
        throw new BusinessException(ProductErrorCode.COMPANY_SERVICE_UNAVAILABLE);

    }

    @Override
    public ApiResponse<CompanyClientResponse> getCompany(UUID companyId) {
        log.warn("[CompanyFeignClient Fallback] Company Service 응답 없음. companyId={}", companyId);
        throw new BusinessException(ProductErrorCode.COMPANY_SERVICE_UNAVAILABLE);
    }
}
