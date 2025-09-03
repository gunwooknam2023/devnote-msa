package com.example.devnote.security;

import com.example.devnote.config.JwtTokenProvider;
import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 JWT 발급·쿠키 저장
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redis;

    /**
     * 로그인 성공 시 JWT 발급 후 쿠키 저장하고, 프론트로 Redirect
     */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 1. Authentication 객체에서 직접 PrincipalDetails 정보 조회
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser(); // DB 조회 없이 바로 User 객체 사용

        // 2. JWT 토큰을 발급
        String accessToken = tokenProvider.createAccessToken(user.getEmail());
        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        // 3. Redis에 리프레시 토큰을 저장
        String redisKey = "refresh:" + refreshToken;
        long refreshValiditySeconds = tokenProvider.getRefreshValidity() / 1000;
        redis.opsForValue().set(redisKey, user.getEmail(), Duration.ofSeconds(refreshValiditySeconds));

        // 4. HttpOnly 쿠키에 토큰을 설정
        setTokenCookies(response, accessToken, refreshToken);

        // 5. 프론트엔드로 리다이렉트
        response.sendRedirect(frontendUrl);
    }

    /**
     * Access/Refresh 토큰을 HttpOnly 쿠키로 설정하는 메서드
     */
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        long accessExpSeconds = tokenProvider.getAccessValidity() / 1000;
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(false) // 운영(HTTPS) 환경에서는 true로 변경
                .path("/")
                .maxAge(accessExpSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        long refreshExpSeconds = tokenProvider.getRefreshValidity() / 1000;
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // 운영(HTTPS) 환경에서는 true로 변경
                .path("/")
                .maxAge(refreshExpSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}