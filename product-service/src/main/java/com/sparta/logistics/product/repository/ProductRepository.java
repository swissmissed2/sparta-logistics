package com.sparta.logistics.product.repository;

import com.sparta.logistics.product.entity.Product;
import com.sparta.logistics.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * deleted_at IS NULL 조건은 @SQLRestriction이 자동 적용
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * 상품 목록 조회 검색 조건
     * - 상품명(부분일치), 업체ID, 허브ID, 상태 필터
     */
    @Query("""
        SELECT p FROM Product p
        WHERE (:name IS NULL OR p.name LIKE %:name%)
        AND (:companyId IS NULL OR p.companyId = :companyId)
        AND (:hubId IS NULL OR p.hubId = :hubId)
        AND (:status IS NULL OR p.status = :status)        
        """)
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("companyId") UUID companyId,
            @Param("hubId") UUID hubId,
            @Param("status")ProductStatus status,
            Pageable pageable
    );

    /**
     * 업체 삭제 시 해당 업체 소속 상품 일괄 soft delete 처리
     *
     * 기존: 상품 목록 조회 후 개별 update 처리 (N+1 문제)
     * 개선: companyId 기준 bulk update로 단일 쿼리 처리하여 DB 부하 감소
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.deletedAt = :deletedAt, p.deletedBy = :deletedBy
        WHERE p.companyId = :companyId
        AND p.deletedAt IS NULL
        """)
    void bulkDeleteByCompanyId(
            @Param("companyId") UUID companyId,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") UUID deletedBy
    );
}
