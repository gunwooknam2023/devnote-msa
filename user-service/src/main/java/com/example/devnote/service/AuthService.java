package com.example.devnote.service;

import com.example.devnote.config.JwtTokenProvider;
import com.example.devnote.dto.TokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 리프레시 토큰 쿠키 기반 재발급 로직
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redis;

    public TokenResponseDto refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid Refresh Token");
        }
        String redisKey = "refresh:" + refreshToken;
        String email = redis.opsForValue().get(redisKey);
        if (email == null) {
            throw new RuntimeException("Refresh Token not found or expired");
        }
        // 새 액세스 토큰 발급
        String newAccess = tokenProvider.createAccessToken(email);
        return new TokenResponseDto("Bearer", newAccess, null);
    }
}
