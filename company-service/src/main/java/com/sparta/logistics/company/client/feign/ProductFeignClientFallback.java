package com.sparta.logistics.company.client.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Product Service 장애 시 Circuit Breaker 대신 기본 Fallback 구현
 * 업체 삭제는 이미 완료된 상태 — 상품 삭제 실패는 로그만 남기고 예외 미발생
 * CompanyService.deleteCompany()의 try-catch에서 처리됨
 */
@Slf4j
@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    @Override
    public void deleteProductsByCompanyId(UUID companyId) {
        log.warn("[ProductFeignClient Fallback] Product Service 응답 없음. companyId={}", companyId);
    }
}
