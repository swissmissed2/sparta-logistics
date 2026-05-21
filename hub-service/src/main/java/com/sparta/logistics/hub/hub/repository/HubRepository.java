package com.sparta.logistics.hub.hub.repository;

import com.sparta.logistics.hub.hub.entity.Hub;
import com.sparta.logistics.hub.hub.enums.HubStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    Optional<Hub> findByIdAndDeletedAtIsNull(UUID hubId);

    boolean existsByName(String name);

    boolean existsByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameAndIdNot(String name, UUID id);

    @Query("SELECT h FROM Hub h WHERE " +
            "(:name IS NULL OR h.name LIKE %:name%) AND " +
            "(:address IS NULL OR h.address LIKE %:address%) AND " +
            "(:status IS NULL OR h.status = :status) AND " +
            "h.deletedAt IS NULL")
    Page<Hub> findAllByCondition(
            @Param("name") String name,
            @Param("address") String address,
            @Param("status") HubStatus status,
            Pageable pageable);
}
