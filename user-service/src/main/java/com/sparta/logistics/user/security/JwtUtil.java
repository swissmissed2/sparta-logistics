package com.sparta.logistics.user.security;

import com.sparta.logistics.common.domain.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtUtil {

    public static final String REFRESH_TOKEN = "refresh";
    public static final long ACCESS_VALID_TIME = 15 * 60 * 1000L; // 로그아웃 기능이 따로 없어 유효시간 15분으로 제한
    public static final long REFRESH_VALID_TIME = 5L * 24 * 60 * 60 * 1000L; // 리프레시 유효 시간 (5일) = 자동 로그아웃 기간

    @Value("${jwt.secret-key}")
    private String secretKey;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // 액세스 토큰 생성
    public String createAccessToken(String userId, Role role, String hubId, String companyId) {
        return createAccessToken(userId, role,hubId, companyId, Duration.ofMillis(ACCESS_VALID_TIME));
    }

    // 액세스 토큰 생성 (유효시간 커스텀)
    public String createAccessToken(String userId, Role role, String hubId, String companyId,  Duration validity) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + validity.toMillis());

        var builder = Jwts.builder()
                .subject(userId)
                .claim("auth", role)
                .claim("token_type", "access")
                .expiration(exp)
                .issuedAt(now)
                .signWith(key);

        if (hubId != null)
            builder.claim("hubId",hubId);

        if (companyId != null)
            builder.claim("companyId",companyId);

        return builder.compact();

    }


    // 리프레시 토큰 생성
    public String createRefreshToken(String userId, Role role, String hubId, String companyId) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + REFRESH_VALID_TIME);

        var builder = Jwts.builder()
                .subject(userId)
                .claim("auth", role)
                .claim("token_type", "refresh")
                .expiration(exp)
                .issuedAt(now)
                .signWith(key);

        if (hubId != null)
            builder.claim("hubId",hubId);

        if (companyId != null)
            builder.claim("companyId",companyId);

        return builder.compact();
    }


    public Optional<Claims> parseClaimsIfMatchType(String token, String expectedTokenType) {
        return getUserInfoFromToken(token)
                .filter(claims -> {
                    boolean match = expectedTokenType.equals(claims.get("token_type"));
                    if (!match) {
                        log.warn("토큰 타입 불일치. 예상: {}", expectedTokenType);
                    }
                    return match;
                });
    }

    // 파싱 , 검증
    public Optional<Claims> getUserInfoFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("토큰 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }


}
