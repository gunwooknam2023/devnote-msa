package com.example.devnote.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret}")
    private String secret;          // 시크릿 키

    @Value("${jwt.access-token-expire}")
    private long accessExpireMs;    // 엑세스 토큰 만료기간

    @Value("${jwt.refresh-token-expire}")
    private long refreshExpireMs;   // 리프레시 토큰 만료기간

    public String getSecret() {
        return secret;
    }

    public long getAccessExpireMs() {
        return accessExpireMs;
    }

    public long getRefreshExpireMs() {
        return refreshExpireMs;
    }
}
