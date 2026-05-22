package com.sparta.logistics.user.presentation.controller;

import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.user.application.result.Token;
import com.sparta.logistics.user.application.result.UserResult;
import com.sparta.logistics.user.presentation.dto.request.LoginRequest;
import com.sparta.logistics.user.presentation.dto.request.SignupRequest;
import com.sparta.logistics.user.presentation.dto.response.LoginResponse;
import com.sparta.logistics.user.presentation.dto.response.SignupResponse;
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

    // 회원가입 ( 모든 사용자 )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signUp(@Valid @RequestBody SignupRequest request) {

        UserResult userResult = authService.signUp(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("회원가입 요청이 완료되었습니다.", SignupResponse.from(userResult)));
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
                .body(ApiResponse.ok(LoginResponse.from(token.accessToken() ,token.userResult())));
    }


    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken){

        Token token = authService.refresh(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.accessToken());
        headers.set("X-Refresh-Token", token.refreshToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.ok(LoginResponse.from(token.accessToken() ,token.userResult())));
    }



}
