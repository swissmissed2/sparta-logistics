package com.sparta.logistics.company.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.company.client.feign.HubCacheService;
import com.sparta.logistics.company.client.feign.HubFeignClient;
import com.sparta.logistics.company.client.feign.ProductFeignClient;
import com.sparta.logistics.company.exception.CompanyErrorCode;
import com.sparta.logistics.company.dto.request.CreateRequest;
import com.sparta.logistics.company.dto.request.SearchCondition;
import com.sparta.logistics.company.dto.request.UpdateRequest;
import com.sparta.logistics.company.dto.response.CompanyResponse;
import com.sparta.logistics.company.dto.response.DeleteResponse;
import com.sparta.logistics.company.entity.Company;
import com.sparta.logistics.company.entity.CompanyStatus;
import com.sparta.logistics.company.entity.CompanyType;
import com.sparta.logistics.company.repository.CompanyRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final HubFeignClient hubFeignClient;
    private final HubCacheService hubCacheService;
    private final ProductFeignClient productFeignClient;

    // -------------------------------------------------------
    // 생성: MASTER, HUB_MANAGER(담당 허브)
    // -------------------------------------------------------
    @Transactional
    public CompanyResponse createCompany(
            CreateRequest request,
            Role requestUserRole,
            UUID requestUserHubId
    ) {
        // 권한 검증
        validateCreatePermission(requestUserRole, requestUserHubId, request.hubId());
        // Hub 존재 여부 검증
        validateHubExists(request.hubId());

        Company company = Company.builder()
                .name(request.name())
                .type(parseCompanyType(request.type()))
                .hubId(request.hubId())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();

        return toResponse(companyRepository.save(company));
    }

    // -------------------------------------------------------
    // 목록 조회 + 검색: ALL
    // -------------------------------------------------------
    public Page<CompanyResponse> searchCompanies (
            SearchCondition condition,
            Pageable pageable) {

        Page<Company> companies = companyRepository.searchCompanies(
                condition.name(),
                parseCompanyType(condition.type()),
                condition.hubId(),
                parseCompanyStatus(condition.status()),
                pageable
        );

        // 중복 제거 후 한 번만 Feign 호출
        List<UUID> hubIds = companies.stream()
                .map(Company::getHubId)
                .distinct()
                .toList();

        Map<UUID, String> hubNameMap = hubCacheService.getHubNameMap(hubIds);

        return companies.map(company ->
                toResponse(company, hubNameMap.get(company.getHubId())));
    }

    // -------------------------------------------------------
    // 단건 조회: ALL
    // -------------------------------------------------------
    public CompanyResponse getCompany(UUID companyId) {
        Company company = findActiveCompanyOrThrow(companyId);
        return toResponse(company);
    }

    // -------------------------------------------------------
    // 수정
    // : MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(본인 업체)
    // -------------------------------------------------------
    @Transactional
    public CompanyResponse updateCompany(
            UUID companyId,
            UpdateRequest request,
            Role requestUserRole,
            UUID requestUserHubId,
            UUID requestUserCompanyId) {

        Company company = findActiveCompanyOrThrow(companyId);

        // 권한 검증
        validateUpdatePermission(
                requestUserRole, requestUserHubId, requestUserCompanyId, company);

        // hubId 변경 시 존재 여부 재검증
        if (request.hubId() != null
                && !request.hubId().equals(company.getHubId())) {
            validateHubExists(request.hubId());
        }

        company.update(
                request.name(),
                parseCompanyType(request.type()),
                request.hubId(),
                request.address(),
                request.latitude(),
                request.longitude(),
                parseCompanyStatus(request.status())
        );

        return toResponse(company);
    }

    // -------------------------------------------------------
    // 삭제 (Soft Delete): MASTER, HUB_MANAGER(담당 허브)
    // -------------------------------------------------------
    @Transactional
    public DeleteResponse deleteCompany(
            UUID companyId,
            UUID requestUserId,
            Role requestUserRole,
            UUID requestUserHubId) {

        Company company = findActiveCompanyOrThrow(companyId);
        validateDeletePermission(requestUserRole, requestUserHubId, company);

        company.delete(requestUserId);

        // 연관 상품 일괄 Soft Delete 요청
        // 실패해도 업체 삭제는 유지 (보상 트랜잭션 범위 밖)
        // → SA 문서 기준 Orchestration Saga 미적용 범위이므로 try-catch로 처리
        try {
            productFeignClient.deleteProductsByCompanyId(companyId);
        } catch (Exception e) {
            log.error("[CompanyService] 연관 상품 삭제 실패. companyId={}", companyId, e);
        }

        log.info("[CompanyService] 업체 논리 삭제. companyId={}, deletedBy={}",
                companyId, requestUserId);

        return new DeleteResponse(company.getId(), company.getDeletedAt());
    }

    // -------------------------------------------------------
    // 내부 서비스 통신용 (Product Service FeignClient 호출 대상)
    // -------------------------------------------------------
    public boolean existsById(UUID companyId) {

        return companyRepository.existsById(companyId);
    }

    // -------------------------------------------------------
    // 검증 메서드
    // -------------------------------------------------------
    private Company findActiveCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));
    }

    private void validateHubExists(UUID hubId) {
        try {
            // Hub Service FeignClient 호출
            hubFeignClient.checkHubExists(hubId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(CompanyErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            throw new BusinessException(CompanyErrorCode.HUB_SERVICE_UNAVAILABLE);
        }
    }

    private void validateCreatePermission(Role role, UUID userHubId, UUID targetHubId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            // 자신의 담당 허브 소속 업체만 생성 가능
            if (userHubId != null && userHubId.equals(targetHubId)) return;
        }
        throw new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED);
    }

    private void validateUpdatePermission(
            Role role, UUID userHubId, UUID userCompanyId, Company company) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER && userHubId != null
                && userHubId.equals(company.getHubId())) return;
        if (role == Role.COMPANY_MANAGER && userCompanyId != null
                && userCompanyId.equals(company.getId())) return;
        throw new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED);
    }

    private void validateDeletePermission(Role role, UUID userHubId, Company company) {
        if(role == Role.MASTER) return;
        if(role == Role.HUB_MANAGER && userHubId != null
                && userHubId.equals(company.getHubId())) return;
        throw new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED);
    }

    // -------------------------------------------------------
    // Enum Parsing
    // -------------------------------------------------------
    private CompanyType parseCompanyType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return CompanyType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CompanyErrorCode.INVALID_COMPANY_TYPE);
        }
    }

    private CompanyStatus parseCompanyStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return CompanyStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CompanyErrorCode.INVALID_COMPANY_STATUS);
        }
    }

    // -------------------------------------------------------
    // Entity -> Response DTO 변환
    // -------------------------------------------------------
    // hubName 없는 경우 (Fallback용)
    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getType().name(),       // Enum → String
                company.getHubId(),
                null,                           // hubName
                company.getAddress(),
                company.getLatitude(),
                company.getLongitude(),
                company.getStatus().name(),     // Enum → String
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }

    // hubName 포함 (목록/단건 조회용)
    private CompanyResponse toResponse(Company company, String hubName) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getType().name(),
                company.getHubId(),
                hubName,
                company.getAddress(),
                company.getLatitude(),
                company.getLongitude(),
                company.getStatus().name(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
