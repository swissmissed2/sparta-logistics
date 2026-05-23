package com.sparta.logistics.order.client;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.order.client.response.CompanyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service", fallback = CompanyServiceClientFallback.class)
public interface CompanyServiceClient {

    @GetMapping("/api/v1/companies/{companyId}/exists")
    void checkCompanyExists(@PathVariable("companyId") UUID companyId);

    @GetMapping("/api/v1/companies/{companyId}")
    ApiResponse<CompanyResponse> getCompany(@PathVariable("companyId") UUID companyId);
}
