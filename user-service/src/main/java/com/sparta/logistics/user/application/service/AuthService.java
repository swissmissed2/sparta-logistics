package com.sparta.logistics.user.application.service;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.user.application.command.LoginCommand;
import com.sparta.logistics.user.application.command.SignupCommand;
import com.sparta.logistics.user.application.result.Token;
import com.sparta.logistics.user.application.result.UserResult;
import com.sparta.logistics.user.domain.model.entity.UserEntity;
import com.sparta.logistics.user.domain.model.enums.UserStatus;
import com.sparta.logistics.user.domain.repository.UserRepository;
import com.sparta.logistics.user.exception.UserErrorCode;
import com.sparta.logistics.user.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원가입
    @Transactional
    public UserResult signUp(SignupCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(command.password());

        UserEntity user = command.toEntity(encodedPassword);

        UserEntity saveUser = userRepository.save(user);

        return UserResult.from(saveUser);
    }


    // 로그인
    @Transactional
    public Token login(LoginCommand command) {

        UserEntity user = userRepository.findByUsernameAndDeletedAtIsNull(command.username())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_NOT_MATCH);
        }

        if (user.getStatus() != UserStatus.APPROVED) {
            throw new BusinessException(UserErrorCode.USER_NOT_APPROVED);
        }

        user.updateLastLoginAt();

        return issueToken(user);
    }

    // 토큰 갱신
    public Token refresh(String refreshToken) {

        Claims claims = jwtUtil.parseClaimsIfMatchType(refreshToken, JwtUtil.REFRESH_TOKEN)
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_TOKEN));

        // subject = 사용자 UUID
        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN);
        }

        // redis 추가 예정

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.APPROVED){
            throw new BusinessException(UserErrorCode.USER_NOT_APPROVED);
        }

        String hubId = (user.getHubId() != null) ? user.getHubId().toString() : null;
        String companyId = (user.getCompanyId() != null) ? user.getCompanyId().toString() : null;

        String accessToken = jwtUtil.createAccessToken(userId.toString(), user.getRole(),hubId, companyId);
        String newRefreshToken = jwtUtil.createRefreshToken(userId.toString(), user.getRole(), hubId, companyId);

        return new Token(UserResult.from(user), accessToken, newRefreshToken);
    }


    // 토큰 생성
    public Token issueToken(UserEntity user) {
        String userId = user.getId().toString();

        String hubId = (user.getHubId() != null) ? user.getHubId().toString() : null;
        String companyId = (user.getCompanyId() != null) ? user.getCompanyId().toString() : null;

        String accessToken = jwtUtil.createAccessToken(userId, user.getRole(), hubId, companyId);
        String refreshToken = jwtUtil.createRefreshToken(userId, user.getRole(), hubId, companyId);

        return new Token(UserResult.from(user), accessToken, refreshToken);
    }

}
