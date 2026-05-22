package com.sparta.logistics.hub.hubstock.repository;

import com.sparta.logistics.hub.hub.entity.Hub;
import com.sparta.logistics.hub.hubstock.entity.HubStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HubStockRepository extends JpaRepository<HubStock, UUID> {

    boolean existsByHubAndProductIdAndDeletedAtIsNull(Hub hub, UUID productId);

    @Query("SELECT h FROM HubStock h WHERE h.hub.id = :hubId " +
            "AND (:productId IS NULL OR h.productId = :productId) " +
            "AND h.deletedAt IS NULL")
    Page<HubStock> findAllByCondition(
            @Param("hubId") UUID hubId,
            @Param("productId") UUID productId,
            Pageable pageable);

    Optional<HubStock> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HubStock h WHERE h.id = :stockId AND h.deletedAt IS NULL")
    Optional<HubStock> findByIdWithLock(@Param("stockId") UUID stockId);
}
