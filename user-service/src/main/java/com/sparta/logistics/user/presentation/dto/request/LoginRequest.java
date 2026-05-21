package com.sparta.logistics.user.presentation.dto.request;

import com.sparta.logistics.user.application.command.LoginCommand;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest( // 로그인 요청

        @NotBlank(message = "아이디는 필수 입력값 입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수 입력값 입니다.")
        String password
){
    public LoginCommand toCommand(){
    return null;


    }

}
