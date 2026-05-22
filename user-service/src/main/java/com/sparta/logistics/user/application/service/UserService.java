package com.sparta.logistics.user.application.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import com.sparta.logistics.user.domain.repository.UserRepository;
import com.sparta.logistics.user.exception.UserErrorCode;
import com.sparta.logistics.user.presentation.dto.request.UpdateRequest;
import com.sparta.logistics.user.presentation.dto.response.ApproveResponse;
import com.sparta.logistics.user.presentation.dto.response.DeleteResponse;
import com.sparta.logistics.user.presentation.dto.response.GetResponse;
import com.sparta.logistics.user.presentation.dto.response.UpdateResponse;
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


    // 마스터 승인
    @Transactional
    public ApproveResponse approveUserByMaster(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.approve();
        return ApproveResponse.from(user);
    }

    // 허브 매니저 승인
    @Transactional
    public ApproveResponse approveUserByHub(UUID userId, UUID hubId) {

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if(!hubId.equals(user.getHubId()))
            throw new BusinessException(UserErrorCode.HUB_MISMATCH);
        user.approve();
        return ApproveResponse.from(user);
    }

    // 마스터 거절
    @Transactional
    public ApproveResponse rejectUserByMaster(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.reject();
        return ApproveResponse.from(user);
    }

    // 허브 매니저 거절
    @Transactional
    public ApproveResponse rejectUserByHub(UUID userId, UUID hubId) {

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if(!hubId.equals(user.getHubId()))
            throw new BusinessException(UserErrorCode.HUB_MISMATCH);

        user.reject();
        return ApproveResponse.from(user);
    }

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


    // 사용자 수정
    @Transactional
    public UpdateResponse updateUser(UUID userId, UpdateRequest request) {

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));

        user.update(request.name(),request.email(),request.slackId(),request.role(),request.hubId(),request.companyId());

        return UpdateResponse.from(user);
    }

    // 사용자 삭제
    @Transactional
    public DeleteResponse deleteUser(UUID userId, UUID requesterId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.softDelete(requesterId);

        return DeleteResponse.from(user);
    }


}
