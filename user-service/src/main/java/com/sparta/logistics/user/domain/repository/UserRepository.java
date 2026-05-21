package com.sparta.logistics.user.domain.repository;

import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.presentation.dto.response.GetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {


    boolean existsByUsername(String username);

    Optional<UserEntity> findByUsername(String username);
}
