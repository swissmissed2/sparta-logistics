package com.sparta.logistics.user.user.repository;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.user.entity.UserEntity;
import com.sparta.logistics.user.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {


    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.deletedAt IS NULL")
    boolean existsAny();

    Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<UserEntity> findAllByHubIdAndDeletedAtIsNull(UUID hubId);

    List<UserEntity> findAllByCompanyIdAndDeletedAtIsNull(UUID companyId);

    @Query("SELECT u FROM UserEntity u WHERE " +
            "u.deletedAt IS NULL AND " +
            "(:username IS NULL OR u.username LIKE %:username%) AND " +
            "(:name IS NULL OR u.name LIKE %:name%) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:status IS NULL OR u.status = :status)")
    Page<UserEntity> searchUsers(
            @Param("username") String username,
            @Param("name") String name,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable);
}
