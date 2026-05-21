package com.sparta.logistics.hub.hub.entity;

import com.sparta.logistics.common.domain.BaseEntity;
import com.sparta.logistics.hub.hub.enums.HubStatus;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "p_hub", schema = "schema_hub")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hub extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HubStatus status = HubStatus.ACTIVE;

    public static Hub create(String name, String address, BigDecimal latitude, BigDecimal longitude) {
        return Hub.builder()
                .name(name)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    public void update(String name, String address, BigDecimal latitude, BigDecimal longitude, HubStatus status) {

        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public void delete(UUID userId) {
        super.softDelete(userId);
    }
}
