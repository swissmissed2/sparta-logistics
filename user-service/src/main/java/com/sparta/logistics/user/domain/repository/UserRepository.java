package com.sparta.logistics.user.domain.repository;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {


    boolean existsByUsername(String username);

    Optional<UserEntity> findByUsername(String username);

    @Query("SELECT u FROM UserEntity u WHERE " +
            "(:username IS NULL OR u.username = :username) AND " +
            "(:name IS NULL OR u.name = :name) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:status IS NULL OR u.status = :status)")
    Page<UserEntity> searchUsers(
            @Param("username") String username,
            @Param("name") String name,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable);
}
