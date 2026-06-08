package com.sparta.logistics.user.user.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.user.auth.repository.RefreshTokenRepository;
import com.sparta.logistics.user.user.entity.UserEntity;
import com.sparta.logistics.user.user.enums.UserStatus;
import com.sparta.logistics.user.user.repository.UserRepository;
import com.sparta.logistics.user.exception.UserErrorCode;
import com.sparta.logistics.user.user.dto.request.UpdateRequest;
import com.sparta.logistics.user.user.dto.response.DeleteResponse;
import com.sparta.logistics.user.user.dto.response.GetResponse;
import com.sparta.logistics.user.user.dto.response.UpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 전체 조회
    public Page<GetResponse> getUsers(String username, String name, Role role, UserStatus status, Pageable pageable) {
        return userRepository.searchUsers(username, name, role, status, pageable)
                .map(GetResponse::from);
    }

    // 유저 조회
    public GetResponse getUser(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        return GetResponse.from(user);
    }

    // 유저 존재 여부 확인 (내부 서비스용)
    public void checkUserExists(UUID userId) {
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    // 사용자 수정
    @Transactional
    public UpdateResponse updateUser(UUID userId, UpdateRequest request) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (request.email() != null
                && !request.email().equals(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.update(request.name(), request.email(), request.slackId(), request.role(), request.hubId(), request.companyId());
        return UpdateResponse.from(user);
    }

    // 허브 삭제 cascade — 소속 사용자 일괄 soft-delete
    @Transactional
    public void softDeleteUsersByHubId(UUID hubId, UUID deletedBy) {
        userRepository.findAllByHubIdAndDeletedAtIsNull(hubId).forEach(user -> {
            user.softDelete(deletedBy);
            refreshTokenRepository.delete(user.getId().toString());
        });
    }

    // 업체 삭제 cascade — 소속 사용자 일괄 soft-delete
    @Transactional
    public void softDeleteUsersByCompanyId(UUID companyId, UUID deletedBy) {
        userRepository.findAllByCompanyIdAndDeletedAtIsNull(companyId).forEach(user -> {
            user.softDelete(deletedBy);
            refreshTokenRepository.delete(user.getId().toString());
        });
    }

    // 사용자 삭제
    @Transactional
    public DeleteResponse deleteUser(UUID userId, UUID requesterId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.softDelete(requesterId);
        refreshTokenRepository.delete(userId.toString());
        return DeleteResponse.from(user);
    }
}
