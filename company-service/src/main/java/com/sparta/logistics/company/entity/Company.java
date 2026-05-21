package com.sparta.logistics.company.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 업체 (p_company)
 * - 모든 업체는 특정 허브에 소속
 * - 생산업체(PRODUCER) / 수령업체(RECEIVER)로 구분
 * - Soft Delete: deleted_at IS NULL인 행만 기본 조회
 */
@Entity
@Table(name = "p_company", schema = "schema_company")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id",updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private CompanyType type;

    /** 관리 허브 ID — Hub Service 간접 참조 */
    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    /** 위도 — 도전기능(AI 경로 최적화) 시 사용 */
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    /** 경도 */
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CompanyStatus status;

    // -------------------------------------------------------
    // 생성 팩토리
    // -------------------------------------------------------
    @Builder
    private Company(String name, CompanyType type, UUID hubId,
                    String address, BigDecimal latitude, BigDecimal longitude) {
        this.name = name;
        this.type = type;
        this.hubId = hubId;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = CompanyStatus.ACTIVE;
    }

    // -------------------------------------------------------
    // 도메인 행동
    // -------------------------------------------------------
    public void update(String name, CompanyType type, UUID hubId,
                       String address, BigDecimal latitude, BigDecimal longitude,
                       CompanyStatus status) {
        if (name != null) this.name = name;
        if (type != null) this.type = type;
        if (hubId != null) this.hubId = hubId;
        if (address != null) this.address = address;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (status != null) this.status = status;
    }

    public void delete(UUID deletedBy) {
        softDelete(deletedBy);
    }
}
