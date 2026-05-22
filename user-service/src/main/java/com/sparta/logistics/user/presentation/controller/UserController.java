package com.sparta.logistics.user.presentation.controller;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.user.application.service.UserService;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import com.sparta.logistics.user.exception.UserErrorCode;
import com.sparta.logistics.user.presentation.dto.request.UpdateRequest;
import com.sparta.logistics.user.presentation.dto.response.ApproveResponse;
import com.sparta.logistics.user.presentation.dto.response.DeleteResponse;
import com.sparta.logistics.user.presentation.dto.response.GetResponse;
import com.sparta.logistics.user.presentation.dto.response.UpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.sparta.logistics.common.domain.Role.HUB_MANAGER;
import static com.sparta.logistics.common.domain.Role.MASTER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50);

    // 가입 승인 (MASTER, HUB_MANAGER)
    @PatchMapping("/approve/{userId}")
    public ResponseEntity<ApiResponse<ApproveResponse>> approveUser(@PathVariable("userId") UUID userId,
                                                                    @RequestHeader("X-User-Role") Role role,
                                                                    @RequestHeader(value = "X-User-HubId", required = false) UUID hubId)
    {
        if(MASTER.equals(role)){
            ApproveResponse response = userService.approveUserByMaster(userId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }

        else if(HUB_MANAGER.equals(role)) {
            if (hubId == null) throw new BusinessException(UserErrorCode.ACCESS_DENIED);
            ApproveResponse response = userService.approveUserByHub(userId, hubId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }

    // 가입 거절 (MASTER, HUB_MANAGER)
    @PatchMapping("/reject/{userId}")
    public ResponseEntity<ApiResponse<ApproveResponse>> rejectUser(@PathVariable("userId") UUID userId,
                                                          @RequestHeader("X-User-Role") Role role,
                                                          @RequestHeader(value = "X-User-HubId", required = false) UUID hubId)
    {
        if(MASTER.equals(role)){
            ApproveResponse response = userService.rejectUserByMaster(userId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }

        else if(HUB_MANAGER.equals(role)) {
            if (hubId == null) throw new BusinessException(UserErrorCode.ACCESS_DENIED);
            ApproveResponse response = userService.rejectUserByHub(userId, hubId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }

    // 전체 정보 조회 (MASTER)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<GetResponse>>> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader("X-User-Role") Role requestRole
    ) {
        if (!ALLOWED_PAGE_SIZES.contains(pageable.getPageSize())) {
            throw new BusinessException(UserErrorCode.INVALID_PAGE_SIZE);
        }
        if(MASTER.equals(requestRole)){
            Page<GetResponse> response = userService.getUsers(username,name,role,status, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }

    // 사용자 단건 조회 (MASTER, 본인)
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<GetResponse>>getUser(@PathVariable UUID userId,
                                                           @RequestHeader("X-User-Id") UUID id,
                                                           @RequestHeader("X-User-Role") Role role){
        if (MASTER.equals(role) || id.equals(userId)){ // MASTER or 본인
            GetResponse response = userService.getUser(userId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }

    // 사용자 수정 (MASTER)
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UpdateResponse>> updateUser(@PathVariable("userId") UUID userId,
                                                                  @RequestHeader("X-User-Role") Role role,
                                                                  @Valid @RequestBody UpdateRequest request
    ){
        if(MASTER.equals(role)){
            UpdateResponse response = userService.updateUser(userId, request);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }

    // 사용자 삭제 (MASTER)
    @DeleteMapping("/{userId}") // MASTER
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteUser(@PathVariable("userId") UUID userId,
                                                                  @RequestHeader("X-User-Role") Role role,
                                                                  @RequestHeader("X-User-Id") UUID requesterId
    ){
        if(MASTER.equals(role)){
            DeleteResponse response = userService.deleteUser(userId, requesterId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }
}

