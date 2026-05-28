package com.sparta.logistics.delivery.repository;

import com.sparta.logistics.delivery.dto.manager.DeliveryManagerSearchCond;
import com.sparta.logistics.delivery.entity.DeliveryManagerEntity;
import com.sparta.logistics.delivery.entity.enums.DeliveryManagerStatus;
import com.sparta.logistics.delivery.entity.enums.DeliveryManagerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryManagerRepository extends JpaRepository<DeliveryManagerEntity, UUID> {

    // 라운드 로빈 배정: deliverySequence ASC, lastAssignedAt ASC (null 먼저)
    @Query("SELECT m FROM DeliveryManagerEntity m " +
            "WHERE m.hubId = :hubId AND m.managerType = :managerType AND m.status = :status " +
            "AND m.deletedAt IS NULL " +
            "ORDER BY m.deliverySequence ASC, m.lastAssignedAt ASC NULLS FIRST")
    Optional<DeliveryManagerEntity> findNextAssignee(
            @Param("hubId") UUID hubId,
            @Param("managerType") DeliveryManagerType managerType,
            @Param("status") DeliveryManagerStatus status);

    // 현재 최대 sequence 조회 (신규 등록 시 마지막 순번 배정용)
    @Query("SELECT COALESCE(MAX(m.deliverySequence), -1) FROM DeliveryManagerEntity m " +
            "WHERE m.deletedAt IS NULL")
    int findMaxDeliverySequence();

    @Query("SELECT m FROM DeliveryManagerEntity m WHERE " +
            "(:#{#cond.managerType} IS NULL OR m.managerType = :#{#cond.managerType}) AND " +
            "(:#{#cond.status} IS NULL OR m.status = :#{#cond.status}) AND " +
            "(:#{#cond.authorizedHubId} IS NULL OR m.hubId = :#{#cond.authorizedHubId}) AND " +
            "m.deletedAt IS NULL")
    Page<DeliveryManagerEntity> findAllByCondition(
            @Param("cond") DeliveryManagerSearchCond cond,
            Pageable pageable);

    List<DeliveryManagerEntity> findAllByHubIdAndDeletedAtIsNull(UUID hubId);
}
