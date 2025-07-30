package com.example.devnote.security;

import com.example.devnote.config.JwtTokenProvider;
import com.example.devnote.dto.TokenResponseDto;
import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        // 1) registrationId (google, naver, kakao) 획득
        String registration = ((OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();

        // 2) OAuth2User 로부터 attributes 가져오기
        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        // 3) Factory 로 provider 별 UserInfo 생성
        OAuth2UserInfo info = OAuth2UserInfoFactory.get(registration, attributes);

        // 4) DB 조회 혹은 신규 user 엔티티 생성
        User user = userRepo.findByProviderAndProviderId(registration, info.getId())
                .orElseGet(() -> User.builder()
                        .provider(registration)
                        .providerId(info.getId())
                        .email(info.getEmail())
                        .name(info.getName())
                        .picture(info.getImageUrl())
                        .build());

        // 5) 이름, 사진 최신화 및 저장
        user.setName(info.getName());
        user.setPicture(info.getImageUrl());
        userRepo.save(user);

        // 6) JWT 토큰 발급
        String accessToken  = tokenProvider.createAccessToken(user.getEmail());
        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        // 7) Redis 에 refresh token 저장
        String redisKey = "refresh:" + refreshToken;
        long expirySec = tokenProvider.getRefreshValidity() / 1000;
        redis.opsForValue().set(redisKey, user.getEmail(), Duration.ofSeconds(expirySec));

        // 8) HTTP-Only 쿠키에 refresh token 세팅
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);  // HTTPS 환경이 아니면 false 로 설정 가능
        cookie.setPath("/");
        cookie.setMaxAge((int) expirySec);
        response.addCookie(cookie);

        // 9) Response body 에는 access token 만 JSON 으로 반환
        response.setContentType("application/json;charset=UTF-8");
        TokenResponseDto dto = new TokenResponseDto("Bearer", accessToken, null);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(dto.toJson());
        } catch (Exception e) {
            // 로그 추가 예정
        }
    }
}