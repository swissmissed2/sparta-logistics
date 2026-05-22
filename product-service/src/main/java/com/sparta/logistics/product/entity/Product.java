package com.sparta.logistics.product.entity;


import com.sparta.logistics.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * 상품 (p_product)
 * - 모든 상품은 특정 업체(company_id)와 허브(hub_id)에 소속
 * - Soft Delete: deleted_at IS NULL 기본 조회
 */
@Entity
@Table(name = "p_product", schema = "schema_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** 소속 업체 ID — Company Service 간접 참조 */
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    /** 관리 허브 ID — Hub Service 간접 참조 */
    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    // -------------------------------------------------------
    // 생성 팩토리
    // -------------------------------------------------------
    @Builder
    private Product(String name, UUID companyId, UUID hubId, Long price, String description) {
        this.name = name;
        this.companyId = companyId;
        this.hubId = hubId;
        this.price = price;
        this.description = description;
        this.status = ProductStatus.AVAILABLE;
    }

    // -------------------------------------------------------
    // 도메인 행동
    // -------------------------------------------------------
    public void update(String name, Long price, String description, ProductStatus status) {
        if (name != null)        this.name = name;
        if (price != null)       this.price = price;
        if (description != null) this.description = description;
        if (status != null)      this.status = status;
    }

    public void delete(UUID deletedBy) {
        softDelete(deletedBy);
    }
}
