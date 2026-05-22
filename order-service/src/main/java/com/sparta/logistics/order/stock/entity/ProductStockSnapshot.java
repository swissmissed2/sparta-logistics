package com.sparta.logistics.order.stock.entity;

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
        ProductStockSnapshot snapshot = new ProductStockSnapshot();
        snapshot.productId = productId;
        snapshot.hubId = hubId;
        snapshot.available = available;
        snapshot.hubStockVersion = hubStockVersion;
        snapshot.syncedAt = LocalDateTime.now();
        return snapshot;
    }

    public void update(Integer available, Long hubStockVersion) {
        this.available = available;
        this.hubStockVersion = hubStockVersion;
        this.syncedAt = LocalDateTime.now();
    }
}
