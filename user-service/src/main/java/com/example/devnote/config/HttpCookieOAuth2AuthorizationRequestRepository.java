package com.example.devnote.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/**
 * 세션 대신 쿠키에 OAuth2 AuthorizationRequest를 저장하는 repository
 */
@Component
@Slf4j
public class HttpCookieOAuth2AuthorizationRequestRepository 
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {

        log.info("쿠키에서 AuthorizationRequest 로드 시도");
        log.info("요청 URL: {}", request.getRequestURL());

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            log.info("전송된 쿠키 개수: {}", cookies.length);
            for (Cookie c : cookies) {
                log.info("쿠키: {}={}", c.getName(), 
                    c.getName().equals(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME) 
                        ? "(value 존재, length=" + c.getValue().length() + ")" 
                        : c.getValue());
            }
        } else {
            log.warn("요청에 쿠키가 존재하지 않습니다.");
        }

        return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    try {
                        OAuth2AuthorizationRequest authRequest = deserialize(cookie, OAuth2AuthorizationRequest.class);
                        log.info("AuthorizationRequest 로드 성공: state={}", authRequest.getState());
                        return authRequest;
                    } catch (Exception e) {
                        log.error("AuthorizationRequest 역직렬화 실패", e);
                        return null;
                    }
                })
                .orElseGet(() -> {
                    log.warn("쿠키에 AuthorizationRequest가 없습니다.");
                    return null;
                });
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            log.info("AuthorizationRequest가 null이므로 쿠키 삭제");
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        log.info("AuthorizationRequest 쿠키에 저장: state={}", authorizationRequest.getState());
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME,
                    redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        }
        return authRequest;
    }

    /**
     * 인증 성공/실패 후 관련 쿠키들을 정리
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request,
                                                   HttpServletResponse response) {
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    private java.util.Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return java.util.Optional.of(cookie);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {

        log.info("쿠키 설정 - name: {}, cookieSecure: {}", name, cookieSecure);

        String cookieHeader = String.format(
                "%s=%s; Path=/; Domain=.devnote.kr; Max-Age=%d; HttpOnly; Secure; SameSite=Lax",
                name, value, maxAge
        );
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    String cookieHeader = String.format(
                            "%s=; Path=/; Domain=.devnote.kr; Max-Age=0; HttpOnly; Secure; SameSite=Lax",
                            name
                    );
                    response.addHeader("Set-Cookie", cookieHeader);
                }
            }
        }
    }

    private String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(Cookie cookie, Class<T> cls) {
        return (T) SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.getValue()));
    }
}
