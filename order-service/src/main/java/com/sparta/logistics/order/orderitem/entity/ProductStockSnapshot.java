package com.sparta.logistics.order.orderitem.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_product_stock_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStockSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, name = "product_id")
    private UUID productId;

    @Column(nullable = false, name = "hub_id")
    private UUID hubId;

    @Column(nullable = false)
    private Integer available;

    @Column(nullable = false, name = "hub_stock_version")
    private Long hubStockVersion;

    @Column(nullable = false, name = "synced_at")
    private LocalDateTime syncedAt;

    public static ProductStockSnapshot create(UUID productId, UUID hubId, Integer available, Long hubStockVersion) {
        ProductStockSnapshot productStockSnapshot = new ProductStockSnapshot();
        productStockSnapshot.productId = productId;
        productStockSnapshot.hubId = hubId;
        productStockSnapshot.available = available;
        productStockSnapshot.hubStockVersion = hubStockVersion;
        productStockSnapshot.syncedAt = LocalDateTime.now();
        return productStockSnapshot;
    }
}
