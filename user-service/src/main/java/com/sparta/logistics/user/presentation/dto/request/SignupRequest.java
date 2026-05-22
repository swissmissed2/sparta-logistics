package com.sparta.logistics.user.presentation.dto.request;

import com.sparta.logistics.user.application.command.SignupCommand;
import com.sparta.logistics.common.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record SignupRequest( // 회원가입 요청
        @NotBlank(message = "아이디는 필수 입력값 입니다.")
        @Size(min = 4, max = 10, message = "아이디는 4자 이상, 10자 이하로 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 알파벳 소문자와 숫자로만 구성되어야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수 입력값 입니다.")
        @Size(min = 8, max = 15, message = "비밀번호는 8자 이상, 15자 이하로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,15}$",
                message = "비밀번호는 알파벳 대소문자, 숫자, 특수문자를 각각 최소 1개 이상 포함해야 합니다.")
        String password,

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
        public SignupCommand toCommand(){
                return SignupCommand.builder()
                        .username(username)
                        .password(password)
                        .name(name)
                        .email(email)
                        .slackId(slackId)
                        .role(role)
                        .hubId(hubId)
                        .companyId(companyId)
                        .build();

        }
}
