package com.example.devnote.security;

import java.util.Map;

/**
 * Naver OAuth2UserInfo
 */
@SuppressWarnings("unchecked")
public class NaverOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attrs;
    public NaverOAuth2UserInfo(Map<String, Object> originalAttrs) {
        this.attrs = (Map<String, Object>) originalAttrs.get("response");
    }
    @Override
    public String getId() {
        return (String) attrs.get("id");
    }
    @Override
    public String getName() {
        return (String) attrs.get("nickname");
    }
    @Override
    public String getEmail() {
        return (String) attrs.get("email");
    }
    @Override
    public String getImageUrl() {
        return (String) attrs.get("profile_image");
    }
}