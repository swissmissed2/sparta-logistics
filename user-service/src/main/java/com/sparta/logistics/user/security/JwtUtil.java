package com.sparta.logistics.user.security;

import com.sparta.logistics.common.domain.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtUtil {

    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_KEY = "auth";
    public static final String TOKEN_TYPE_KEY = "token_type";
    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final long ACCESS_VALID_TIME = 15 * 60 * 1000L; // 로그아웃 기능이 따로 없어 유효시간 15분으로 제한
    public static final long REFRESH_VALID_TIME = 35L * 24 * 60 * 60 * 1000L; // 리프레시 유효 시간 (15일)

    @Value("${jwt.secret-key}")
    private String secretKey;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(String userId, Role role) {
        return createAccessToken(userId, role, Duration.ofMillis(ACCESS_VALID_TIME));
    }

    // 요청한 만료 시간을 전달받아 엑세스 토큰 생성
    public String createAccessToken(String userId, Role role, Duration validity) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + validity.toMillis());

        return Jwts.builder()
                .subject(userId)
                .claim(AUTH_KEY, role)
                .claim(TOKEN_TYPE_KEY, ACCESS_TOKEN)
                .expiration(exp)
                .issuedAt(now)
                .signWith(key)
                .compact();
    }


    // 리프레시 토큰 생성
    public String createRefreshToken(String userId, Role role) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + REFRESH_VALID_TIME);

        return Jwts.builder()
                .subject(userId)
                .claim(AUTH_KEY, role)
                .claim(TOKEN_TYPE_KEY, REFRESH_TOKEN)
                .expiration(exp)
                .issuedAt(now)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, ACCESS_TOKEN);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TOKEN);
    }

    /**
     * 서명·만료 검증 후 {@link #TOKEN_TYPE_KEY} 가 {@code expectedTokenType} 과 일치할 때만 클레임을 반환한다.
     * JWT 문자열은 최대 한 번만 파싱한다.
     */
    public Optional<Claims> parseClaimsIfMatchType(String token, String expectedTokenType) {
        try {
            Claims claims = getUserInfoFromToken(token);
            if (!expectedTokenType.equals(claims.get(TOKEN_TYPE_KEY))) {
                log.error("Token type mismatch. Expected: {}", expectedTokenType);
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty.");
        }
        return Optional.empty();
    }

    // 유효성 검사
    public boolean validateToken(String token, String Type) {
        return parseClaimsIfMatchType(token, Type).isPresent();
    }


    // 파싱 , 검증
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }


    public String substringToken(String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {return null;}
        String t = tokenValue.trim();
        if (t.length() >= BEARER_PREFIX.length() && t.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            t = t.substring(BEARER_PREFIX.length()).trim();
        }
        return StringUtils.hasText(t) ? t : null;
    }
}
