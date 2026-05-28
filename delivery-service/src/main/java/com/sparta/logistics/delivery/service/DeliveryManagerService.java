package com.sparta.logistics.delivery.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.delivery.client.HubServiceClient;
import com.sparta.logistics.delivery.dto.manager.DeliveryManagerCreateRequest;
import com.sparta.logistics.delivery.dto.manager.DeliveryManagerResponse;
import com.sparta.logistics.delivery.dto.manager.DeliveryManagerSearchCond;
import com.sparta.logistics.delivery.dto.manager.DeliveryManagerStatusChangeRequest;
import com.sparta.logistics.delivery.dto.manager.DeliveryManagerUpdateRequest;
import com.sparta.logistics.delivery.entity.DeliveryManagerEntity;
import com.sparta.logistics.delivery.entity.enums.DeliveryManagerStatus;
import com.sparta.logistics.delivery.entity.enums.DeliveryManagerType;
import com.sparta.logistics.delivery.exception.DeliveryErrorCode;
import com.sparta.logistics.delivery.repository.DeliveryManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryManagerService {

    private final DeliveryManagerRepository managerRepository;
    private final DeliveryPermissionChecker permissionChecker;
    private final HubServiceClient hubServiceClient;

    // 배송담당자 생성
    @Transactional
    public DeliveryManagerResponse createManager(DeliveryManagerCreateRequest req,
                                                  UUID actorId, Role role, UUID hubId) {
        permissionChecker.checkManagerWritePermission(req.hubId(), actorId, role, hubId);

        // 멱등성 보장: 동일 userId 중복 등록 방어 (userId가 @Id — PK 중복 방지)
        if (managerRepository.existsById(req.userId())) {
            throw new BusinessException(DeliveryErrorCode.MANAGER_ALREADY_EXISTS);
        }

        // COMPANY_DELIVERY 타입은 hub-service Feign으로 허브 존재 검증
        if (req.managerType() == DeliveryManagerType.COMPANY_DELIVERY) {
            hubServiceClient.checkHubExists(req.hubId());
        }

        int nextSequence = managerRepository.findMaxDeliverySequence() + 1;

        DeliveryManagerEntity entity = new DeliveryManagerEntity(
                req.userId(), req.hubId(), req.slackId(), req.managerType(), nextSequence
        );
        return DeliveryManagerResponse.from(managerRepository.save(entity));
    }

    // 배송담당자 목록 조회
    @Transactional(readOnly = true)
    public Page<DeliveryManagerResponse> getManagerList(UUID userId, Role role, UUID hubId, Pageable pageable,
                                                         DeliveryManagerSearchCond cond) {
        return switch (role) {
            case MASTER ->
                managerRepository.findAllByCondition(cond, pageable)
                        .map(DeliveryManagerResponse::from);
            case HUB_MANAGER -> {
                if (hubId == null) throw new BusinessException(DeliveryErrorCode.HUB_NOT_FOUND);
                cond.setAuthorizedHubId(hubId);
                yield managerRepository.findAllByCondition(cond, pageable)
                        .map(DeliveryManagerResponse::from);
            }
            case DELIVERY_MANAGER -> {
                DeliveryManagerEntity self = findActiveOrThrow(userId);
                yield new PageImpl<>(List.of(DeliveryManagerResponse.from(self)), pageable, 1);
            }
            default -> throw new BusinessException(com.sparta.logistics.common.exception.CommonErrorCode.FORBIDDEN);
        };
    }

    // 배송담당자 단건 조회
    @Transactional(readOnly = true)
    public DeliveryManagerResponse getManager(UUID managerId, UUID userId, Role role, UUID hubId) {
        DeliveryManagerEntity entity = findActiveOrThrow(managerId);
        permissionChecker.checkManagerReadPermission(entity, userId, role, hubId);
        return DeliveryManagerResponse.from(entity);
    }

    // 배송담당자 수정
    @Transactional
    public DeliveryManagerResponse updateManager(UUID managerId, DeliveryManagerUpdateRequest req,
                                                  UUID userId, Role role, UUID hubId) {
        DeliveryManagerEntity entity = findActiveOrThrow(managerId);
        permissionChecker.checkManagerSelfWritePermission(entity, userId, role, hubId);
        entity.updateInfo(req.hubId() != null ? req.hubId() : entity.getHubId(),
                          req.slackId() != null ? req.slackId() : entity.getSlackId());
        return DeliveryManagerResponse.from(entity);
    }

    // 배송담당자 상태변경
    @Transactional
    public DeliveryManagerResponse changeManagerStatus(UUID managerId, DeliveryManagerStatusChangeRequest req,
                                                        UUID userId, Role role, UUID hubId) {
        DeliveryManagerEntity entity = findActiveOrThrow(managerId);
        permissionChecker.checkManagerSelfWritePermission(entity, userId, role, hubId);
        entity.changeStatus(req.status());
        return DeliveryManagerResponse.from(entity);
    }

    // 배송담당자 삭제 (soft delete)
    @Transactional
    public void deleteManager(UUID managerId, UUID actorId, Role role, UUID hubId) {
        DeliveryManagerEntity entity = findActiveOrThrow(managerId);
        permissionChecker.checkManagerWritePermission(entity.getHubId(), actorId, role, hubId);

        // PR-B: manager.status 기반 최소 검증 (PR-D에서 Route 연동으로 보강 가능)
        if (entity.getStatus() == DeliveryManagerStatus.WORKING) {
            throw new BusinessException(DeliveryErrorCode.MANAGER_IN_DELIVERY);
        }
        entity.delete(actorId);
    }

    private DeliveryManagerEntity findActiveOrThrow(UUID managerId) {
        DeliveryManagerEntity entity = managerRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(DeliveryErrorCode.MANAGER_NOT_FOUND));
        if (entity.isDeleted()) {
            throw new BusinessException(DeliveryErrorCode.MANAGER_ALREADY_DELETED);
        }
        return entity;
    }
}
