package com.sparta.logistics.company.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Delete /api/v1/products/by-company/{companyId}
 * 업체 삭제 시 해당 업체의 상품 일괄 비활성화 요청
 */
@FeignClient(name = "product-service", fallback = ProductFeignClientFallback.class)
public interface ProductFeignClient {

    @DeleteMapping("/api/v1/products/by-company/{companyId}")
    void deleteProductsByCompanyId(@PathVariable UUID companyId);

}
