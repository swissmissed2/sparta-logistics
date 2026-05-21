package com.sparta.logistics.hub.hub.controller;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.hub.hub.dto.request.UpdateHubRequest;
import com.sparta.logistics.hub.hub.dto.response.*;
import com.sparta.logistics.hub.hub.dto.request.CreateHubRequest;
import com.sparta.logistics.hub.hub.enums.HubStatus;
import com.sparta.logistics.hub.hub.service.HubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    /**
     * 허브 생성 api
     * todo: @PreAuthorize("hasRole('MASTER')") 적용 - X-User-Role 헤더 기반 SecurityContext 세팅 필터 추가 후
     * @param request 허브 생성 요청 DTO
     * @return 생성된 허브 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HubCreateResponse>> createHub(@RequestBody @Valid CreateHubRequest request) {


        HubCreateResponse response = hubService.createHub(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("허브가 생성되었습니다.", response));
    }

    /**
     * 허브 목록 조회 api
     * todo: @PreAuthorize("isAuthenticated()") 적용 - X-User-Role 헤더 기반 SecurityContext 세팅 필터 추가 후
     * @param name
     * @param address
     * @param status
     * @param pageable
     * @return 허브 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<HubListResponse>>> getHubList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) HubStatus status,
            Pageable pageable) {

        Page<HubListResponse> response = hubService.getHubList(name, address, status, pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 허브 단건 조회 api
     * todo: @PreAuthorize("isAuthenticated()") 적용 - X-User-Role 헤더 기반 SecurityContext 세팅 필터 추가 후
     * @param hubId 허브 ID
     * @return 허브 상세 정보
     */
    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubDetailResponse>> getHub(@PathVariable UUID hubId) {

        HubDetailResponse response = hubService.getHub(hubId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 허브 존재 여부 확인 api (내부 서비스 간 통신용)
     * @param hubId 허브 ID
     * @return 200 OK (존재), 404 Not Found (없음)
     */
    @GetMapping("/{hubId}/exists")
    public ResponseEntity<Void> isHubExists(@PathVariable UUID hubId) {

        boolean exists = hubService.existsHub(hubId);

        if (!exists)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok().build();
    }

    /**
     * 허브 수정 api
     * todo: @PreAuthorize("hasRole('MASTER')") 적용 - X-User-Role 헤더 기반 SecurityContext 세팅 필터 추가 후
     * @param hubId 허브 ID
     * @param request 허브 수정 요청 DTO
     * @return 수정된 허브 정보
     */
    @PutMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubUpdateResponse>> updateHub(@PathVariable UUID hubId,
                                                                    @RequestBody @Valid UpdateHubRequest request) {

        HubUpdateResponse response = hubService.updateHub(hubId, request);

        return ResponseEntity.ok(ApiResponse.ok("허브가 수정되었습니다.", response));
    }

    /**
     * 허브 삭제 api
     * todo: @PreAuthorize("hasRole('MASTER')") 적용 - X-User-Role 헤더 기반 SecurityContext 세팅 필터 추가 후
     * @param hubId 허브 ID
     * @param userId 삭제 요청자 ID (X-User-Id 헤더)
     * @return 삭제된 허브 정보
     */
    @DeleteMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubDeleteResponse>> deleteHub(
            @PathVariable UUID hubId,
            @RequestHeader("X-User-Id") UUID userId) {

        HubDeleteResponse response = hubService.deleteHub(hubId, userId);

        return ResponseEntity.ok(ApiResponse.ok("허브가 삭제되었습니다.", response));
    }
}
