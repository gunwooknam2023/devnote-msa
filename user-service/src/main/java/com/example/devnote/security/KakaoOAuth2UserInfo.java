package com.example.devnote.security;

import java.util.Map;

/**
 * Kakao OAuth2UserInfo
 */
@SuppressWarnings("unchecked")
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attrs;
    private final Map<String, Object> properties;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> accountProfile;

    public KakaoOAuth2UserInfo(Map<String, Object> attrs) {
        this.attrs = attrs;
        this.properties = (Map<String, Object>) attrs.get("properties");
        this.kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
        this.accountProfile = kakaoAccount != null
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : null;

        System.out.println("=== kakao attrs ===");
        attrs.forEach((k,v) -> System.out.println(k + " = " + v));
    }

    @Override
    public String getId() {
        return String.valueOf(attrs.get("id"));
    }

    @Override
    public String getName() {
        // properties.nickname 우선, 없으면 account.profile.nickname
        if (properties != null && properties.get("nickname") != null) {
            return (String) properties.get("nickname");
        }
        if (accountProfile != null) {
            return (String) accountProfile.get("nickname");
        }
        return null;
    }

    @Override
    public String getEmail() {
        return kakaoAccount != null
                ? (String) kakaoAccount.get("email")
                : null;
    }

    @Override
    public String getImageUrl() {
        // properties.profile_image 우선
        if (properties != null && properties.get("profile_image") != null) {
            return (String) properties.get("profile_image");
        }
        // 없으면 kakao_account.profile.profile_image_url
        if (accountProfile != null && accountProfile.get("profile_image_url") != null) {
            return (String) accountProfile.get("profile_image_url");
        }
        return null;
    }
}