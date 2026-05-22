package com.sparta.logistics.hub.hubstocklog.repository;

import com.sparta.logistics.hub.hubstock.enums.HubStockChangeType;
import com.sparta.logistics.hub.hubstocklog.entity.HubStockLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface HubStockLogRepository extends JpaRepository<HubStockLog, UUID> {

    @Query("SELECT l FROM HubStockLog l WHERE l.hubStock.id = :stockId " +
            "AND l.hubStock.hub.id = :hubId " +
            "AND (:changeType IS NULL OR l.changeType = :changeType) " +
            "AND l.deletedAt IS NULL")
    Page<HubStockLog> findAllByCondition(
            @Param("stockId") UUID stockId,
            @Param("hubId") UUID hubId,
            @Param("changeType") HubStockChangeType changeType,
            Pageable pageable);
}
