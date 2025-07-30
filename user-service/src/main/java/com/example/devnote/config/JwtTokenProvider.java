package com.example.devnote.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 생성·검증 유틸리티
 */
@Component
@Getter
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity-ms}")
    private long accessValidity;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshValidity;

    /** 액세스 토큰 생성 */
    public String createAccessToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessValidity))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /** 리프레시 토큰 생성 */
    public String createRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshValidity))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /** 토큰 검증 */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** 토큰에서 사용자(subject) 추출 */
    public String getUsername(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}