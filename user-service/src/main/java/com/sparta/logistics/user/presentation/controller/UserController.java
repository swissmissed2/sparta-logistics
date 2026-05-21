package com.sparta.logistics.user.presentation.controller;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.user.application.service.AuthService;
import com.sparta.logistics.user.application.service.UserService;
import com.sparta.logistics.user.exception.UserErrorCode;
import com.sparta.logistics.user.presentation.dto.response.GetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

}
