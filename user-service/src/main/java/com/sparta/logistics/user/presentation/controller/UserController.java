package com.sparta.logistics.user.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    //현재 서버의 환경 설정 정보
    private final Environment env;

    @GetMapping("/api/users/data")
    public String getUserData(){

        String port = env.getProperty("local.server.port");
        return String.format("user-service(실제 할당된 랜덤 포트: %s)에서 정상적으로 처리해서 보낸 응답 데이터", port);
    }
}
