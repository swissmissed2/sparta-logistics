package com.sparta.logistics.order.stock.repository;

import com.sparta.logistics.order.stock.entity.ProductStockSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductStockSnapshotRepository extends JpaRepository<ProductStockSnapshot, UUID> {

    Optional<ProductStockSnapshot> findByProductId(UUID productId);
}
