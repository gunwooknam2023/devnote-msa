package com.example.devnote.config;

import com.example.devnote.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRefreshFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws ServletException, IOException {
        // 1) accessToken 쿠키에서 토큰 추출
        String token = Arrays.stream(req.getCookies() != null ? req.getCookies() : new Cookie[0])
                .filter(c -> "accessToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);

        if (token != null) {
            try {
                tokenProvider.validateToken(token);
            } catch (ExpiredJwtException ex) {
                log.info("Access token expired, attempting refresh…");

                // 2) refreshToken 쿠키 추출
                String refresh = Arrays.stream(req.getCookies())
                        .filter(c -> "refreshToken".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);

                // 3) 유효한 리프레시 토큰이면
                if (refresh != null && tokenProvider.validateToken(refresh)) {
                    var dto = authService.refresh(refresh);
                    String newAccess = dto.getAccessToken();

                    // 4) 새 accessToken 을 쿠키로 세팅
                    long expSec = tokenProvider.getAccessValidity() / 1000;
                    ResponseCookie cookie = ResponseCookie.from("accessToken", newAccess)
                            .httpOnly(true)
                            .secure(false)
//                            .sameSite("None")
                            .path("/")
                            .maxAge(expSec)
                            .build();
                    res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                    // 5) SecurityContext 에 인증 정보 다시 세팅
                    String username = tokenProvider.getUsername(newAccess);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.info("Access token refreshed for user {}", username);
                }
            }
        }

        chain.doFilter(req, res);
    }

    /**
     * 이 필터를 적용하지 않을 경로를 지정
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // /internal/** 경로로 들어오는 요청은 이 필터를 건너뜁니다.
        return request.getRequestURI().startsWith("/internal/");
    }
}