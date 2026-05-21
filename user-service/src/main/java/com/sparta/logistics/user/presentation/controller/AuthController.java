package com.sparta.logistics.user.presentation.controller;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.user.application.result.Token;
import com.sparta.logistics.user.application.result.UserResult;
import com.sparta.logistics.user.presentation.dto.request.LoginRequest;
import com.sparta.logistics.user.presentation.dto.request.SignupRequest;
import com.sparta.logistics.user.presentation.dto.response.UserResponse;
import com.sparta.logistics.user.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    // 회원가입 //201
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signUp(@Valid @RequestBody SignupRequest request) {

        UserResult userResult = authService.signUp(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("회원가입 요청이 완료되었습니다.",UserResponse.from(userResult)));
    }

    // 로그인 //200
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request){

        Token token = authService.login(request.toCommand());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.accessToken());
        headers.set("X-Refresh-Token", token.refreshToken());


        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.ok(UserResponse.from(token.userResult())));
    }


    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UserResponse>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken){

        Token token = authService.refresh(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.accessToken());
        headers.set("X-Refresh-Token", token.refreshToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.ok(UserResponse.from(token.userResult())));
    }

    // user-service에서 구현
//    // 가입 승인 (MASTER, HUB_MANAGER) //200
//    @PatchMapping("/signup/{userId}/approve")
//    public ResponseEntity<ApiResponse<ApproveResponse>> approveUser(@PathVariable("userId") UUID userId, Authentication auth ) {
//        ApproveResponse response = authService.approveUser(userId);
//        return ResponseEntity.ok(ApiResponse.ok(response));
//    }

    // 가입 거절 (MASTER, HUB_MANAGER) // user-service에서 구현
//    @PatchMapping("/signup/{userId}/reject")
//    public ResponseEntity<Map<String, String>> rejectUser(@PathVariable("userId") UUID userId) {
//        authService.rejectUser(userId);
//
//        // "reason" 이라는 key로 JSON 바디가 생성됨
//        return ResponseEntity.ok(Map.of("reason", "소속 정보 불일치"));
//    }


}
