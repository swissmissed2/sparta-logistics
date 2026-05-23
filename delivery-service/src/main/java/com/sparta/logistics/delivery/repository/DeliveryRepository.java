package com.sparta.logistics.delivery.repository;

import com.sparta.logistics.delivery.dto.DeliverySearchCond;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {
    // TODO: QueryDSL 로 변경
    @Query("SELECT d FROM DeliveryEntity d WHERE " +
            "(:#{#cond.orderId} IS NULL OR d.orderId = :#{#cond.orderId}) AND " +
            "(:#{#cond.status} IS NULL OR d.status = :#{#cond.status}) AND " +
            "(:#{#cond.sourceHubId} IS NULL OR d.sourceHubId = :#{#cond.sourceHubId}) AND " +
            "(:#{#cond.destinationHubId} IS NULL OR d.destinationHubId = :#{#cond.destinationHubId}) AND " +
            "(:#{#cond.deliveryManagerId} IS NULL OR d.deliveryManagerId = :#{#cond.deliveryManagerId}) AND " +
            "(:#{#cond.authorizedHubId} IS NULL OR (d.sourceHubId = :#{#cond.authorizedHubId} OR d.destinationHubId = :#{#cond.authorizedHubId})) AND " +
            "(:#{#cond.authorizedManagerId} IS NULL OR d.deliveryManagerId = :#{#cond.authorizedManagerId}) AND " +
            "d.deletedAt IS NULL")
    Page<DeliveryEntity> findAllByCondition(
            @Param("cond") DeliverySearchCond cond,
            Pageable pageable);
}