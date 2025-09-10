package com.example.devnote.controller;

import com.example.devnote.config.JwtTokenProvider;
import com.example.devnote.dto.ApiResponseDto;
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
    private final JwtTokenProvider tokenProvider;

    /**
     * 리프레시 토큰으로 액세스 토큰 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<TokenResponseDto>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        // 1) refreshToken 검증 & 새 액세스 토큰 발급
        TokenResponseDto dto = authService.refresh(refreshToken);

        // 2) Access Token 을 HttpOnly 쿠키로 세팅
        long accessExpSec = tokenProvider.getAccessValidity() / 1000;
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", dto.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("None")
                .path("/")
                .maxAge(accessExpSec)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // 3) (선택) 응답 바디에도 토큰 포함—클라이언트에서 필요시 사용
        //    필요 없다면 dto.setAccessToken(null) 하고 반환하세요.
        return ResponseEntity.ok(
                ApiResponseDto.<TokenResponseDto>builder()
                        .message("Token refreshed")
                        .statusCode(200)
                        .data(dto)
                        .build()
        );
    }

    /**
     * 로그아웃: 리프레시 토큰 삭제 + 쿠키 만료(리프레시/액세스 토큰 모두)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1) refreshToken 쿠키 값 읽기
        String refresh = Arrays.stream(request.getCookies() != null
                        ? request.getCookies()
                        : new jakarta.servlet.http.Cookie[0])
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(c -> c.getValue())
                .findFirst()
                .orElse(null);

        // 2) Redis 에서 리프레시 토큰 삭제
        if (refresh != null) {
            redis.delete("refresh:" + refresh);
        }

        // 3) refreshToken 쿠키 만료
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        // 4) accessToken 쿠키도 만료
        ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());

        return ResponseEntity.ok().build();
    }
}