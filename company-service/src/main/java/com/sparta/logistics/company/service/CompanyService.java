package com.sparta.logistics.company.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.company.client.feign.HubCacheService;
import com.sparta.logistics.company.client.feign.HubFeignClient;
import com.sparta.logistics.company.common.exception.CompanyErrorCode;
import com.sparta.logistics.company.dto.request.CreateRequest;
import com.sparta.logistics.company.dto.request.SearchCondition;
import com.sparta.logistics.company.dto.request.UpdateRequest;
import com.sparta.logistics.company.dto.response.CompanyResponse;
import com.sparta.logistics.company.dto.response.DeleteResponse;
import com.sparta.logistics.company.entity.Company;
import com.sparta.logistics.company.repository.CompanyRepository;
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

    // -------------------------------------------------------
    // 생성: MASTER, HUB_MANAGER(담당 허브)
    // -------------------------------------------------------
    @Transactional
    public CompanyResponse createCompany(
            CreateRequest request,
            UUID requestUserId,
            Role requestUserRole,
            UUID requestUserHubId
    ) {
        // 권한 검증
        validateCreatePermission(requestUserRole, requestUserHubId, request.hubId());
        // Hub 존재 여부 검증
        validateHubExists(request.hubId());

        Company company = Company.builder()
                .name(request.name())
                .type(request.type())
                .hubId(request.hubId())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();

        return CompanyResponse.from(companyRepository.save(company));
    }

    // -------------------------------------------------------
    // 목록 조회 + 검색: ALL
    // -------------------------------------------------------
    public Page<CompanyResponse> searchCompanies (
            SearchCondition condition,
            Pageable pageable) {

        Page<Company> companies = companyRepository.searchCompanies(
                condition.name(),
                condition.type(),
                condition.hubId(),
                condition.status(),
                pageable
        );

        // 중복 제거 후 한 번만 Feign 호출
        List<UUID> hubIds = companies.stream()
                .map(Company::getHubId)
                .distinct()
                .toList();

        Map<UUID, String> hubNameMap = hubCacheService.getHubNameMap(hubIds);

        return companies.map(company -> CompanyResponse.of(
                company,
                hubNameMap.get(company.getHubId())
        ));
    }

    // -------------------------------------------------------
    // 단건 조회: ALL
    // -------------------------------------------------------
    public CompanyResponse getCompany(UUID companyId) {
        Company company = findActiveCompanyOrThrow(companyId);
        return CompanyResponse.from(company);
    }

    // -------------------------------------------------------
    // 수정
    // : MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(본인 업체)
    // -------------------------------------------------------
    @Transactional
    public CompanyResponse updateCompany(
            UUID companyId,
            UpdateRequest request,
            UUID requestUserId,
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
                request.type(),
                request.hubId(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.status()
        );

        return CompanyResponse.from(company);
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
        // Hub Service FeignClient 호출
        boolean exists = hubFeignClient.checkHubExists(hubId).exists();
        // 존재 X → 400
        if (!exists) {
            throw new BusinessException(CompanyErrorCode.HUB_NOT_FOUND);
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
}
