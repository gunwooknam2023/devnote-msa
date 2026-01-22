package com.example.devnote.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;

/**
 * 쿠키 관련 유틸리티
 * - OAuth2 인증 요청을 쿠키에 저장/조회/삭제하기 위해 사용
 */
public class CookieUtils {

    /**
     * 요청에서 특정 이름의 쿠키를 찾아 반환
     * @param request HTTP 요청
     * @param name 찾을 쿠키 이름
     * @return 쿠키 Optional
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 응답에 쿠키 추가
     * @param response HTTP 응답
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 쿠키 유효 시간(초)
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        // HTTPS 환경이면 true로 설정 (프로덕션에서는 true 권장)
        // cookie.setSecure(true);
        response.addCookie(cookie);
    }

    /**
     * 특정 쿠키 삭제 (maxAge를 0으로 설정)
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param name 삭제할 쿠키 이름
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * 객체를 직렬화하여 Base64 문자열로 변환
     * OAuth2AuthorizationRequest 객체를 쿠키에 저장하기 위해 사용
     */
    public static String serialize(Serializable object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    /**
     * Base64 문자열을 역직렬화하여 객체로 복원
     * 쿠키에서 OAuth2AuthorizationRequest 객체를 복원하기 위해 사용
     * @param cookie 쿠키
     * @param cls 복원할 클래스 타입
     * @return 역직렬화된 객체
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}
