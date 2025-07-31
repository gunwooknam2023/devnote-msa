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
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

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
    private final UserRepository userRepo;
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
        // 1) provider
        String reg = ((OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();

        // 2) attributes
        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = principal.getAttributes();

        // 3) to DTO
        OAuth2UserInfo info = OAuth2UserInfoFactory.get(reg, attrs);

        // 4) user 저장/업데이트
        User user = userRepo.findByProviderAndProviderId(reg, info.getId())
                .orElseGet(() -> User.builder()
                        .provider(reg)
                        .providerId(info.getId())
                        .email(info.getEmail())
                        .name(info.getName())
                        .picture(info.getImageUrl())
                        .build()
                );
        user.setName(info.getName());
        user.setPicture(info.getImageUrl());
        userRepo.save(user);

        // 5) JWT 발급
        String accessToken = tokenProvider.createAccessToken(user.getEmail()); // 제거? 다시 한번 생각해야함
        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        // 6) Redis 저장
        String key = "refresh:" + refreshToken;
        long expSec = tokenProvider.getRefreshValidity() / 1000;
        redis.opsForValue().set(key, user.getEmail(), Duration.ofSeconds(expSec));

        // 7) HttpOnly + SameSite=None + Secure 쿠키로 RefreshToken 세팅
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(tokenProvider.getRefreshValidity() / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 8) next → 리다이렉트
        String next = request.getParameter("next");
        String target = (next != null && !next.isEmpty()) ? next : frontendUrl;
        response.sendRedirect(target);
    }
}