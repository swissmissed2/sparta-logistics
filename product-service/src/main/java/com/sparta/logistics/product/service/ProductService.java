package com.sparta.logistics.product.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.product.client.model.CompanyClientResponse;
import com.sparta.logistics.product.client.feign.CompanyFeignClient;
import com.sparta.logistics.product.dto.request.CreateRequest;
import com.sparta.logistics.product.dto.request.SearchCondition;
import com.sparta.logistics.product.dto.request.UpdateRequest;
import com.sparta.logistics.product.dto.response.DeleteResponse;
import com.sparta.logistics.product.dto.response.ProductResponse;
import com.sparta.logistics.product.entity.Product;
import com.sparta.logistics.product.entity.ProductStatus;
import com.sparta.logistics.product.exception.ProductErrorCode;
import com.sparta.logistics.product.repository.ProductRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CompanyFeignClient companyFeignClient;

    // -------------------------------------------------------
    // 생성: MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(본인 업체)
    // -------------------------------------------------------
    @Transactional
    public ProductResponse createProduct(
            CreateRequest request,
            UUID requestUserId,
            Role requestUserRole,
            UUID requestUserHubId,
            UUID requestUserCompanyId) {

        validateCreatePermission(requestUserRole, requestUserHubId,
                requestUserCompanyId, request.hubId(), request.companyId());

        // 업체 조회 후 해당 업체가 실제로 해당 허브 소속인지 정합성 검증
        CompanyClientResponse company = companyFeignClient.getCompany(request.companyId()).data();

        // FeignClient 호출 실패나 Fallback 발생 시 null 반환할 수 있으므로 null 체크
        if (company == null) {
            throw new BusinessException(ProductErrorCode.COMPANY_NOT_FOUND);
        }

        if (!company.hubId().equals(request.hubId())) {
            throw new BusinessException(ProductErrorCode.COMPANY_HUB_MISMATCH);
        }

        Product product = Product.builder()
                .name(request.name())
                .companyId(request.companyId())
                .hubId(request.hubId())
                .price(request.price())
                .description(request.description())
                .build();

        return toResponse(productRepository.save(product));
    }

    // -------------------------------------------------------
    // 목록 조회 + 검색: ALL
    // -------------------------------------------------------
    public Page<ProductResponse> searchProducts(
            SearchCondition condition, Pageable pageable) {

        return productRepository.searchProducts(
                condition.name(),
                condition.companyId(),
                condition.hubId(),
                parseProductStatus(condition.status()),
                pageable).map(this::toResponse);
    }

    // -------------------------------------------------------
    // 단건 조회: ALL
    // -------------------------------------------------------
    public ProductResponse getProduct(UUID productId) {
        Product product = findActiveProductOrThrow(productId);

        return toResponse(product);
    }

    // -------------------------------------------------------
    // 수정: MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(본인 업체)
    // -------------------------------------------------------
    @Transactional
    public ProductResponse updateProduct(
            UUID productId,
            UpdateRequest request,
            Role requestUserRole,
            UUID requestUserHubId,
            UUID requestUserCompanyId) {

        Product product =findActiveProductOrThrow(productId);
        validateUpdatePermission(requestUserRole, requestUserHubId,
                requestUserCompanyId, product);

        product.update(request.name(), request.price(),
                request.description(), parseProductStatus(request.status()));

        return toResponse(product);
    }

    // -------------------------------------------------------
    // 삭제 (Soft Delete): MASTER, HUB_MANAGER(담당 허브)
    // -------------------------------------------------------
    @Transactional
    public DeleteResponse deleteProduct(
            UUID productId,
            UUID requestUserId,
            Role requestUserRole,
            UUID requestUserHubId) {

        Product product = findActiveProductOrThrow(productId);
        validateDeletePermission(requestUserRole, requestUserHubId, product);

        product.delete(requestUserId);
        log.info("[ProductService] 상품 논리 삭제. productId={}, deletedBy={}", productId, requestUserId);

        return new DeleteResponse(product.getId(), product.getDeletedAt());
    }

    // -------------------------------------------------------
    // 상품 일괄 삭제: MASTER, HUB_MANAGER(담당 허브)
    // -------------------------------------------------------
    @Transactional
    public void deleteAllByCompanyId(UUID companyId) {
        List<Product> products = productRepository.findAllByCompanyId(companyId);

        // 시스템 삭제 — deletedBy null 허용
        products.forEach(product -> product.delete(null));

        log.info("[ProductService] 업체 삭제로 인한 상품 일괄 삭제. companyId={}, count={}",
                companyId, products.size());
    }

    // -------------------------------------------------------
    // 검증
    // -------------------------------------------------------

    private Product findActiveProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateCompanyExists(UUID companyId) {
        try {
            companyFeignClient.checkCompanyExists(companyId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ProductErrorCode.COMPANY_NOT_FOUND);
        }
    }

    private void validateCreatePermission(
            Role role, UUID userHubId, UUID userCompanyId,
            UUID targetHubId, UUID targetCompanyId) {

        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER && userHubId != null
                && userHubId.equals(targetHubId)) return;
        if (role == Role.COMPANY_MANAGER && userCompanyId != null) return;

        throw new BusinessException(ProductErrorCode.PRODUCT_ACCESS_DENIED);
    }

    private void validateUpdatePermission(
            Role role, UUID userHubId, UUID userCompanyId, Product product) {

        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER && userHubId != null
                && userHubId.equals(product.getHubId())) return;
        if (role == Role.COMPANY_MANAGER && userCompanyId != null
                && userCompanyId.equals(product.getCompanyId())) return;

        throw new BusinessException(ProductErrorCode.PRODUCT_ACCESS_DENIED);
    }

    private void validateDeletePermission(
            Role role, UUID userHubId, Product product) {

        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER && userHubId != null
                && userHubId.equals(product.getHubId())) return;

        throw new BusinessException(ProductErrorCode.PRODUCT_ACCESS_DENIED);
    }

    // -------------------------------------------------------
    // Enum Parsing
    // -------------------------------------------------------
    private ProductStatus parseProductStatus (String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return ProductStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_STATUS);
        }
    }


    // -------------------------------------------------------
    // Entity -> Response DTO 변환
    // -------------------------------------------------------
    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCompanyId(),
                null,               // companyName — 필요 시 FeignClient 조회
                product.getHubId(),
                null,               // hubName — 필요 시 FeignClient 조회
                product.getPrice(),
                product.getDescription(),
                product.getStatus().name(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
