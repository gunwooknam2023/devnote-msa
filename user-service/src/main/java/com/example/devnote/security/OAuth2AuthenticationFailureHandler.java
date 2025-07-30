package com.example.devnote.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

/**
 * OAuth2 로그인 실패 시 처리
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res,
                                        org.springframework.security.core.AuthenticationException ex) {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        try(PrintWriter w = res.getWriter()) {
            w.write("{\"error\":\"OAuth2 Authentication Failed\"}");
        } catch (Exception ignore) {}
    }
}
