package com.example.devnote.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2 인증 요청을 쿠키에 저장하는 Repository
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    // OAuth2 인증 요청을 저장할 쿠키 이름
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";

    // 리다이렉트 URI를 저장할 쿠키 이름 (로그인 후 돌아갈 페이지)
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";

    // 쿠키 만료 시간 (초) - 3분
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    /**
     * 쿠키에서 OAuth2 인증 요청을 로드
     * OAuth 제공자에서 콜백이 올 때 호출됨
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    /**
     * OAuth2 인증 요청을 쿠키에 저장
     * /oauth2/authorize/{provider} 요청 시 호출됨
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            // 인증 요청이 null이면 관련 쿠키들을 삭제
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        // OAuth2AuthorizationRequest를 직렬화하여 쿠키에 저장
        CookieUtils.addCookie(response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtils.serialize(authorizationRequest),
                COOKIE_EXPIRE_SECONDS);

        // redirect_uri 파라미터가 있으면 별도 쿠키에 저장 (로그인 후 돌아갈 페이지)
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
            CookieUtils.addCookie(response,
                    REDIRECT_URI_PARAM_COOKIE_NAME,
                    redirectUriAfterLogin,
                    COOKIE_EXPIRE_SECONDS);
        }
    }

    /**
     * 인증 완료 후 쿠키에서 OAuth2 인증 요청을 제거하고 반환
     * 인증 성공/실패 핸들러에서 호출됨
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
        // 쿠키 삭제는 removeAuthorizationRequestCookies()에서 별도로 처리
        return authorizationRequest;
    }

    /**
     * OAuth2 관련 쿠키들을 모두 삭제
     * 인증 성공/실패 핸들러에서 호출해야 함
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request,
                                                  HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
