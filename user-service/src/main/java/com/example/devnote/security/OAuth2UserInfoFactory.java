package com.example.devnote.security;

import java.util.Map;

/**
 * registrationId 에 따라 OAuth2UserInfo 구현체 생성
 */
public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo get(String registrationId, Map<String,Object> attrs) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attrs);
            case "naver"  -> new NaverOAuth2UserInfo(attrs);
            case "kakao"  -> new KakaoOAuth2UserInfo(attrs);
            default       -> throw new IllegalArgumentException("지원하지 않는 소셜: " + registrationId);
        };
    }
}