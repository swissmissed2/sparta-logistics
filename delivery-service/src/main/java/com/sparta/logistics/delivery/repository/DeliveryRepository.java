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

    // 멱등성 보장: Kafka at-least-once 중복 소비 방어용
    // orderId 단독 체크는 다중 허브 주문에서 두 번째 이벤트를 중복으로 차단하므로
    // sourceHubId 조합으로 변경 — 같은 주문의 같은 출발 허브 조합만 중복 처리
    boolean existsByOrderIdAndSourceHubId(UUID orderId, UUID sourceHubId);
    // TODO: QueryDSL 로 변경
    @Query("SELECT d FROM DeliveryEntity d WHERE " +
            "(:#{#cond.orderId} IS NULL OR d.orderId = :#{#cond.orderId}) AND " +
            "(:#{#cond.status} IS NULL OR d.status = :#{#cond.status}) AND " +
            "(:#{#cond.sourceHubId} IS NULL OR d.sourceHubId = :#{#cond.sourceHubId}) AND " +
            "(:#{#cond.destinationHubId} IS NULL OR d.destinationHubId = :#{#cond.destinationHubId}) AND " +
            "(:#{#cond.companyDeliveryManagerId} IS NULL OR d.companyDeliveryManagerId = :#{#cond.companyDeliveryManagerId}) AND " +
            "(:#{#cond.authorizedHubId} IS NULL OR (d.sourceHubId = :#{#cond.authorizedHubId} OR d.destinationHubId = :#{#cond.authorizedHubId})) AND " +
            "(:#{#cond.authorizedManagerId} IS NULL OR d.companyDeliveryManagerId = :#{#cond.authorizedManagerId}) AND " +
            "(:#{#cond.authorizedOrderIds} IS NULL OR d.orderId IN :#{#cond.authorizedOrderIds}) AND " +
            "d.deletedAt IS NULL")
    Page<DeliveryEntity> findAllByCondition(
            @Param("cond") DeliverySearchCond cond,
            Pageable pageable);
}