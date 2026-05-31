package com.sparta.logistics.delivery.service;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.exception.CommonErrorCode;
import com.sparta.logistics.delivery.client.FeignCallService;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.DeliveryManagerEntity;
import com.sparta.logistics.delivery.entity.DeliveryRouteEntity;
import com.sparta.logistics.delivery.repository.DeliveryRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryPermissionChecker {

    private final DeliveryRouteRepository routeRepository;
    private final FeignCallService feignCallService;

    // 배송 단건 조회 — 권한 없으면 403
    public void checkDeliveryReadPermission(DeliveryEntity delivery, UUID userId, Role role,
                                            UUID hubId, UUID companyId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            if (hubId != null && (hubId.equals(delivery.getSourceHubId()) || hubId.equals(delivery.getDestinationHubId()))) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role == Role.DELIVERY_MANAGER) {
            if (userId != null && userId.equals(delivery.getCompanyDeliveryManagerId())) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role == Role.COMPANY_MANAGER) {
            if (companyId != null && delivery.getOrderId() != null
                    && feignCallService.checkOrderBelongsToCompany(delivery.getOrderId(), companyId)) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 배송 수정 / 상태변경 — MASTER, HUB_MANAGER(자기 허브)만 허용
    // DELIVERY_MANAGER는 상태변경만 허용 (별도 메서드로 분리)
    public void checkDeliveryWritePermission(DeliveryEntity delivery, UUID userId, Role role, UUID hubId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            if (hubId != null && (hubId.equals(delivery.getSourceHubId()) || hubId.equals(delivery.getDestinationHubId()))) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 배송 상태변경 — MASTER, HUB_MANAGER(자기 허브), DELIVERY_MANAGER(자신 담당) 허용
    public void checkDeliveryStatusChangePermission(DeliveryEntity delivery, UUID userId, Role role, UUID hubId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            if (hubId != null && (hubId.equals(delivery.getSourceHubId()) || hubId.equals(delivery.getDestinationHubId()))) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role == Role.DELIVERY_MANAGER) {
            // 업체 배송 담당자 체크
            if (userId != null && userId.equals(delivery.getCompanyDeliveryManagerId())) return;
            // 허브 배송 담당자 체크: 해당 delivery의 구간(route) 담당자인 경우도 허용
            if (userId != null && routeRepository.existsByDelivery_IdAndHubDeliveryManagerId(delivery.getId(), userId)) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 배송 삭제 — MASTER만 허용
    public void checkDeletePermission(Role role) {
        if (role != Role.MASTER) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    // 배송담당자 접근 권한 — MASTER, HUB_MANAGER(자기 허브), DELIVERY_MANAGER(본인만)
    public void checkManagerReadPermission(DeliveryManagerEntity manager, UUID userId, Role role, UUID hubId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            if (hubId != null && hubId.equals(manager.getHubId())) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role == Role.DELIVERY_MANAGER) {
            if (userId != null && userId.equals(manager.getId())) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 배송담당자 생성/수정/삭제 — MASTER, HUB_MANAGER(자기 허브)만
    public void checkManagerWritePermission(UUID targetHubId, UUID userId, Role role, UUID hubId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            if (hubId != null && hubId.equals(targetHubId)) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 허브 변경 가능 여부 — MASTER만 허용
    public boolean canChangeHubId(Role role) {
        return role == Role.MASTER;
    }

    // 배송담당자 본인 수정 — MASTER, HUB_MANAGER(자기 허브), DELIVERY_MANAGER(본인)
    public void checkManagerSelfWritePermission(DeliveryManagerEntity manager, UUID userId, Role role, UUID hubId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            if (hubId != null && hubId.equals(manager.getHubId())) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role == Role.DELIVERY_MANAGER) {
            if (userId != null && userId.equals(manager.getId())) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 배송경로 수정 — MASTER, HUB_MANAGER(자기 허브 배송), DELIVERY_MANAGER(자신 담당 구간)
    public void checkRouteWritePermission(DeliveryEntity delivery, DeliveryRouteEntity route,
                                          UUID userId, Role role, UUID hubId) {
        if (role == Role.MASTER) return;
        if (role == Role.HUB_MANAGER) {
            if (hubId != null && (hubId.equals(delivery.getSourceHubId()) || hubId.equals(delivery.getDestinationHubId()))) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role == Role.DELIVERY_MANAGER) {
            if (userId != null && userId.equals(route.getHubDeliveryManagerId())) return;
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
}
