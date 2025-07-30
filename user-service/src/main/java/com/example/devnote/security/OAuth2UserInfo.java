package com.example.devnote.security;

/**
 * OAuth2 사용자 정보 추상화 인터페이스
 */
public interface OAuth2UserInfo {
    String getId();
    String getName();
    String getEmail();
    String getImageUrl();
}
