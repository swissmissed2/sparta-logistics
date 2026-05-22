package com.sparta.logistics.user.presentation.dto.request;

import com.sparta.logistics.common.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateRequest(

        @NotBlank(message = "이름은 필수 입력 값입니다.")
        @Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
        String name,

        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하로 입력해주세요.")
        String email,

        @NotBlank(message = "slackId는 필수 입력 값입니다.")
        @Size(max = 255, message = "slackId는 255자 이하로 입력해주세요.")
        String slackId,

        @NotNull(message = "권한은 필수 입력 값입니다.")
        Role role,

        UUID hubId,
        UUID companyId
) {
}

