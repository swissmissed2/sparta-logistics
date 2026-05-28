package com.sparta.logistics.user.presentation.controller;

import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.user.application.result.Token;
import com.sparta.logistics.user.application.result.UserResult;
import com.sparta.logistics.user.application.service.AuthService;
import com.sparta.logistics.user.exception.UserErrorCode;
import com.sparta.logistics.user.presentation.dto.request.LoginRequest;
import com.sparta.logistics.user.presentation.dto.request.SignupRequest;
import com.sparta.logistics.user.presentation.dto.response.ApproveResponse;
import com.sparta.logistics.user.presentation.dto.response.LoginResponse;
import com.sparta.logistics.user.presentation.dto.response.SignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.sparta.logistics.common.domain.Role.HUB_MANAGER;
import static com.sparta.logistics.common.domain.Role.MASTER;


import com.sparta.logistics.user.application.validator.HubCompanyValidator;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final HubCompanyValidator hubCompanyValidator;

    // 회원가입 ( 모든 사용자 )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signUp(@Valid @RequestBody SignupRequest request) {

        hubCompanyValidator.validate(request.hubId(), request.companyId());

        UserResult userResult = authService.signUp(request.toCommand());

        String message = MASTER.equals(userResult.role())
                ? "관리자 계정으로 가입이 완료되었습니다."
                : "회원가입 요청이 완료되었습니다. 관리자 승인을 기다려 주세요.";

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(message, SignupResponse.from(userResult)));
    }

    // 로그인 ( 승인된 사용자 )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request){

        Token token = authService.login(request.toCommand());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.accessToken()); // Bearer + accessToken
        headers.set("X-Refresh-Token", token.refreshToken()); // refreshToken


        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.ok("로그인이 성공적으로 완료되었습니다..",LoginResponse.from(token.accessToken() ,token.userResult())));
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken){

        Token token = authService.refresh(refreshToken.trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.accessToken());
        headers.set("X-Refresh-Token", token.refreshToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.ok("토큰이 성공적으로 재발급되었습니다.", LoginResponse.from(token.accessToken() ,token.userResult())));
    }

    // 회원가입 승인 (MASTER, HUB_MANAGER)
    @PatchMapping("/signup/{userId}/approve")
    public ResponseEntity<ApiResponse<ApproveResponse>> approveUser(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Role") Role role,
            @RequestHeader(value = "X-User-HubId", required = false) UUID hubId
    ) {
        if (MASTER.equals(role)) {
            ApproveResponse response = authService.approveUserByMaster(userId);
            return ResponseEntity.ok(ApiResponse.ok("승인 처리가 완료되었습니다.", response));
        }
        if (HUB_MANAGER.equals(role)) {
            if (hubId == null) throw new BusinessException(UserErrorCode.ACCESS_DENIED);
            ApproveResponse response = authService.approveUserByHub(userId, hubId);
            return ResponseEntity.ok(ApiResponse.ok("승인 처리가 완료되었습니다.", response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }

    // 회원가입 거절 (MASTER, HUB_MANAGER)
    @PatchMapping("/signup/{userId}/reject")
    public ResponseEntity<ApiResponse<ApproveResponse>> rejectUser(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Role") Role role,
            @RequestHeader(value = "X-User-HubId", required = false) UUID hubId
    ) {
        if (MASTER.equals(role)) {
            ApproveResponse response = authService.rejectUserByMaster(userId);
            return ResponseEntity.ok(ApiResponse.ok("승인 신청이 거절되었습니다.", response));
        }
        if (HUB_MANAGER.equals(role)) {
            if (hubId == null) throw new BusinessException(UserErrorCode.ACCESS_DENIED);
            ApproveResponse response = authService.rejectUserByHub(userId, hubId);
            return ResponseEntity.ok(ApiResponse.ok("승인 신청이 거절되었습니다.", response));
        }
        throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }
}
