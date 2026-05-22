package com.sparta.logistics.product.client.feign;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.product.client.model.CompanyClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * GET /api/v1/companies/{companyId}/exists
 * - 상품 생성 시 업체 존재 여부 검증
 * - 상품 생성 시 해당 업체가 실제로 해당 허브 소속인지 정합성 검증
 */
@FeignClient(name = "company-service", fallback = CompanyFeignClientFallback.class)
public interface CompanyFeignClient {
    @GetMapping("/api/v1/companies/{companyId}/exists")
    void checkCompanyExists(@PathVariable("companyId")UUID companyId);

    @GetMapping("/api/v1/companies/{companyId}")
    ApiResponse<CompanyClientResponse> getCompany(@PathVariable UUID companyId);
}
