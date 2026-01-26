package com.example.devnote.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * 표준 OAuth 2.0 제공자(네이버, 카카오 등)의 사용자 정보를 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserProcessor userProcessor;

    /**
     * OAuth2 제공자로부터 액세스 토큰을 받은 후 호출
     * 이 메소드에서 사용자 정보를 가져와 DB에 저장하거나 업데이트
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("OAuth2 loadUser 시작: registrationId={}", userRequest.getClientRegistration().getRegistrationId());

        try {
            // 1. 부모 클래스의 loadUser를 호출하여 기본적인 OAuth2User 객체를 가져옴
            OAuth2User oauth2User = super.loadUser(userRequest);
            String registrationId = userRequest.getClientRegistration().getRegistrationId();

            // 2. 중앙 관리되는 userProcessor를 통해 사용자 정보를 처리 (가입/업데이트)
            var user = userProcessor.processUser(registrationId, oauth2User);

            // 3. 처리된 User 엔티티와 원본 oauth2User 정보를 담은 PrincipalDetails 객체를 생성하여 반환
            // 이 반환된 객체가 Spring Security의 SecurityContext에 저장됨
            return new PrincipalDetails(user, oauth2User);
        } catch (Exception e) {
            log.error("OAuth2 인증 과정 중 에러 발생", e);
            throw e;
        }

    }
}