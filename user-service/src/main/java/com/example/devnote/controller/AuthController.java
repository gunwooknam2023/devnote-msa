package com.example.devnote.controller;

import com.example.devnote.dto.TokenResponseDto;
import com.example.devnote.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 인증 토큰 재발급 API
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final StringRedisTemplate redis;

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        TokenResponseDto dto = authService.refresh(refreshToken);
        return ResponseEntity.ok(dto);
    }

    /** 로그아웃: 리프레시 토큰 삭제 + 쿠키 만료 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1) 쿠키에서 refreshToken 꺼내기
        String token = Arrays.stream(request.getCookies() != null ? request.getCookies() : new jakarta.servlet.http.Cookie[0])
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(c -> c.getValue())
                .findFirst()
                .orElse(null);

        // 2) Redis 에서 삭제
        if (token != null) {
            redis.delete("refresh:" + token);
        }

        // 3) SameSite=None; Secure; HttpOnly 쿠키로 덮어쓰기 (만료시켜서 브라우저에서 삭제)
        ResponseCookie clear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)           // 운영환경은 true, 로컬 HTTPS 아니면 false
                .sameSite("None")       // 프론트가 다른 도메인/포트일때  None
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());

        return ResponseEntity.ok().build();
    }
}