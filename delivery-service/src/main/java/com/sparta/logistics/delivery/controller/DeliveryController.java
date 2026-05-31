package com.sparta.logistics.delivery.controller;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.dto.DeliveryDetailResponse;
import com.sparta.logistics.delivery.dto.DeliveryListResponse;
import com.sparta.logistics.delivery.dto.DeliverySearchCond;
import com.sparta.logistics.delivery.dto.DeliveryStatusChangeRequest;
import com.sparta.logistics.delivery.dto.DeliveryUpdateRequest;
import com.sparta.logistics.delivery.service.DeliveryAssignmentService;
import com.sparta.logistics.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryAssignmentService deliveryAssignmentService;

    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryDetailResponse>> getDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role,
            @RequestHeader(value = "X-User-HubId", required = false) UUID hubId,
            @RequestHeader(value = "X-User-CompanyId", required = false) UUID companyId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getDelivery(deliveryId, userId, role, hubId, companyId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeliveryListResponse>>> getDeliveryList(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role,
            @RequestHeader(value = "X-User-HubId", required = false) UUID hubId,
            @RequestHeader(value = "X-User-CompanyId", required = false) UUID companyId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @ModelAttribute DeliverySearchCond cond
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getDeliveryList(userId, role, hubId, companyId, pageable, cond)));
    }

    @PutMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryDetailResponse>> updateDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role,
            @RequestHeader(value = "X-User-HubId", required = false) UUID hubId,
            @RequestBody DeliveryUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.updateDelivery(deliveryId, request, userId, role, hubId)));
    }

    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<ApiResponse<DeliveryDetailResponse>> changeStatus(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role,
            @RequestHeader(value = "X-User-HubId", required = false) UUID hubId,
            @Valid @RequestBody DeliveryStatusChangeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.changeStatus(deliveryId, request, userId, role, hubId)));
    }

    @DeleteMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role
    ) {
        deliveryService.deleteDelivery(deliveryId, userId, role);
        return ResponseEntity.ok(ApiResponse.ok("배송이 삭제되었습니다.", null));
    }

    // 배송 생성은 Kafka stock.reserved 이벤트를 통해 자동 생성됨 (DeliveryEventHandler 참고)

    /**
     * 수동 배차 API — 배송에 담당자를 라운드 로빈 방식으로 배정한다.
     * 권한: MASTER 전체 허용 / HUB_MANAGER 는 자기 허브 배송만 허용.
     *
     * <p>추후 delivery.created Kafka consumer 에서 호출 시 이 엔드포인트 변경 없이 서비스 메서드만 재사용.
     */
    @PostMapping("/{deliveryId}/assign")
    public ResponseEntity<ApiResponse<Void>> assignManagers(
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") Role role,
            @RequestHeader(value = "X-User-HubId", required = false) UUID hubId
    ) {
        deliveryAssignmentService.assignManagers(deliveryId, userId, role, hubId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
