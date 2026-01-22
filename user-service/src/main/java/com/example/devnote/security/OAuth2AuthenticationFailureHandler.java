package com.example.devnote.security;

import com.example.devnote.config.JwtTokenProvider;
import com.example.devnote.exception.UserWithdrawnException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * OAuth2 로그인 실패 시 처리
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final JwtTokenProvider tokenProvider;

    @Value("${app.frontend-url}")
    private List<String> frontendUrls;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String baseRedirect = frontendUrls.get(0);

        // 커스텀 예외 처리 로직
        if (exception instanceof UserWithdrawnException withdrawnException) {
            // 탈퇴한 사용자일 경우, 임시 토큰을 발급하여 리다이렉트
            String email = withdrawnException.getEmail();

            // 5분짜리 임시 "상태 확인용 토큰" 발급
            String statusToken = tokenProvider.createStatusToken(email);
            String redirectUrl = baseRedirect + "/withdrawn?token=" + statusToken;
            response.sendRedirect(redirectUrl);
            return;
        }

        // 일반적인 로그인 실패 처리
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter w = response.getWriter()) {
            w.write("{\"error\":\"OAuth2 Authentication Failed: " + exception.getMessage() + "\"}");
        } catch (Exception ignore) {}
    }
}
