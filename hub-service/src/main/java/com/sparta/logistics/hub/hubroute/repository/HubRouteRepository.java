package com.sparta.logistics.hub.hubroute.repository;

import com.sparta.logistics.hub.hub.entity.Hub;
import com.sparta.logistics.hub.hubroute.entity.HubRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    boolean existsBySourceHubAndDestinationHubAndDeletedAtIsNull(Hub sourceHub, Hub destinationHub);

    Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID hubRouteId);

    // 허브 목록 조회(fetch join 사용)
    @Query("SELECT r FROM HubRoute r " +
            "JOIN FETCH r.sourceHub " +
            "JOIN FETCH r.destinationHub " +
            "WHERE (:sourceHubId IS NULL OR r.sourceHub.id = :sourceHubId) " +
            "AND (:destinationHubId IS NULL OR r.destinationHub.id = :destinationHubId) " +
            "AND r.deletedAt IS NULL")
    Page<HubRoute> findAllByCondition(
            @Param("sourceHubId") UUID sourceHubId,
            @Param("destinationHubId") UUID destinationHubId,
            Pageable pageable);

    // 허브 경로 단건 조회(허브 이름을 응답해야 하기에 fetch join 사용)
    @Query("SELECT r FROM HubRoute r " +
            "JOIN FETCH r.sourceHub " +
            "JOIN FETCH r.destinationHub " +
            "WHERE r.id = :routeId " +
            "AND r.deletedAt IS NULL")
    Optional<HubRoute> findByIdWithHubs(@Param("routeId") UUID routeId);

    @Query("SELECT r FROM HubRoute r " +
            "WHERE (r.sourceHub = :hub OR r.destinationHub = :hub) " +
            "AND r.deletedAt IS NULL")
    List<HubRoute> findAllByHubAndDeletedAtIsNull(@Param("hub") Hub hub);
}
