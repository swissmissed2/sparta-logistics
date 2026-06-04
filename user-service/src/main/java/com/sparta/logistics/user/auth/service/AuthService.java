package com.sparta.logistics.user.auth.service;

import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.user.auth.internal.command.LoginCommand;
import com.sparta.logistics.user.auth.internal.command.SignupCommand;
import com.sparta.logistics.user.auth.internal.result.Token;
import com.sparta.logistics.user.auth.internal.result.UserResult;
import com.sparta.logistics.user.user.entity.UserEntity;
import com.sparta.logistics.user.user.enums.UserStatus;
import com.sparta.logistics.user.auth.repository.RefreshTokenRepository;
import com.sparta.logistics.user.user.repository.UserRepository;
import com.sparta.logistics.user.exception.UserErrorCode;
import com.sparta.logistics.user.user.dto.response.ApproveResponse;
import com.sparta.logistics.user.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 회원가입
    @Transactional
    public UserResult signUp(SignupCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_EXISTS);
        }

        if (command.email() != null && userRepository.existsByEmail(command.email())) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(command.password());

        UserEntity user = command.toEntity(encodedPassword);

        // 첫 번째 가입자는 MASTER + APPROVED로 자동 설정
        // Advisory lock으로 동시 요청 직렬화 (트랜잭션 종료 시 자동 해제)
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(1)").getResultList();
        if (!userRepository.existsAny()) {
            user.setRoleAndApprove();
        }

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
    @Transactional
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

        String stored = refreshTokenRepository.find(userId.toString())
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_TOKEN));

        if (!stored.equals(refreshToken)) {
            refreshTokenRepository.delete(userId.toString()); // 탈취 의심 → 강제 삭제
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.APPROVED){
            throw new BusinessException(UserErrorCode.USER_NOT_APPROVED);
        }

        String hubId = (user.getHubId() != null) ? user.getHubId().toString() : null;
        String companyId = (user.getCompanyId() != null) ? user.getCompanyId().toString() : null;

        String accessToken = jwtUtil.createAccessToken(userId.toString(), user.getRole(),hubId, companyId);
        String newRefreshToken = jwtUtil.createRefreshToken(userId.toString(), user.getRole(), hubId, companyId);

        refreshTokenRepository.save(userId.toString(), newRefreshToken);

        return new Token(UserResult.from(user), accessToken, newRefreshToken);
    }


    // 토큰 생성
    private Token issueToken(UserEntity user) {
        String userId = user.getId().toString();

        String hubId = (user.getHubId() != null) ? user.getHubId().toString() : null;
        String companyId = (user.getCompanyId() != null) ? user.getCompanyId().toString() : null;

        String accessToken = jwtUtil.createAccessToken(userId, user.getRole(), hubId, companyId);
        String refreshToken = jwtUtil.createRefreshToken(userId, user.getRole(), hubId, companyId);

        refreshTokenRepository.save(userId, refreshToken);

        return new Token(UserResult.from(user), accessToken, refreshToken);
    }

    // 마스터 승인
    @Transactional
    public ApproveResponse approveUserByMaster(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.approve();
        return ApproveResponse.from(user);
    }

    // 허브 매니저 승인
    @Transactional
    public ApproveResponse approveUserByHub(UUID userId, UUID hubId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (!hubId.equals(user.getHubId()))
            throw new BusinessException(UserErrorCode.HUB_MISMATCH);
        user.approve();
        return ApproveResponse.from(user);
    }

    // 마스터 거절
    @Transactional
    public ApproveResponse rejectUserByMaster(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.reject();
        return ApproveResponse.from(user);
    }

    // 허브 매니저 거절
    @Transactional
    public ApproveResponse rejectUserByHub(UUID userId, UUID hubId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (!hubId.equals(user.getHubId()))
            throw new BusinessException(UserErrorCode.HUB_MISMATCH);
        user.reject();
        return ApproveResponse.from(user);
    }

}