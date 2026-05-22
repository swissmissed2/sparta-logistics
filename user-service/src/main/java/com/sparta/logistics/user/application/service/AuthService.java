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
import com.sparta.logistics.user.presentation.dto.response.ApproveResponse;
import com.sparta.logistics.user.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    @Transactional(readOnly = true)
    public Token login(LoginCommand command) {

        UserEntity user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND) {
                });

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_NOT_MATCH) {
            };
        }

        if (user.getStatus() != UserStatus.APPROVED) {
            throw new BusinessException(UserErrorCode.USER_NOT_APPROVED){
            };
        }

        return issueToken(user);

    }

    // 토큰 갱신
    @Transactional
    public Token refresh(String bearerRefreshToken) {

        if (!StringUtils.hasText(bearerRefreshToken)) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN);
        }

        String refreshToken = jwtUtil.substringToken(bearerRefreshToken); // 안전장치
        if (refreshToken == null) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN);
        }

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

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.APPROVED){
            throw new BusinessException(UserErrorCode.USER_NOT_APPROVED);
        }

        String accessToken = jwtUtil.createAccessToken(userId.toString(), user.getRole(),user.getHubId().toString(), user.getCompanyId().toString());
        String newRefreshToken = jwtUtil.createRefreshToken(userId.toString(), user.getRole(), user.getHubId().toString(), user.getCompanyId().toString());

        return new Token(UserResult.from(user), accessToken, newRefreshToken);
    }

    //승인
    @Transactional
    public ApproveResponse approveUser(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.approve();
        return ApproveResponse.approveResponse(user);
    }

    // 거절
    @Transactional
    public void rejectUser(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(()->new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.reject();
    }

    // 토큰 생성
    public Token issueToken(UserEntity user) {
        String userId = user.getId().toString();
        String hubId = user.getHubId().toString();
        String companyId = user.getCompanyId().toString();
        String accessToken = jwtUtil.createAccessToken(userId, user.getRole(), hubId, companyId );
        String refreshToken = jwtUtil.createRefreshToken(userId, user.getRole(), hubId, companyId);

        return new Token(UserResult.from(user), accessToken, refreshToken);
    }

}
