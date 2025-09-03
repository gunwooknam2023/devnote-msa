package com.example.devnote.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * OIDC(OpenID Connect) 1.0 제공자(구글 등)의 사용자 정보를 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final OAuth2UserProcessor userProcessor;

    /**
     * OIDC 제공자로부터 ID 토큰을 받은 후 호출
     * 이 메소드에서 사용자 정보를 가져와 DB에 저장하거나 업데이트
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 부모 클래스의 loadUser를 호출하여 기본적인 OidcUser 객체를 가져옴
        OidcUser oidcUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 2. 중앙 관리되는 userProcessor를 통해 사용자 정보를 처리 (가입/업데이트)
        var user = userProcessor.processUser(registrationId, oidcUser);

        // 3. 처리된 User 엔티티와 원본 oidcUser 정보를 담은 PrincipalDetails 객체를 생성하여 반환
        // 이 반환된 객체가 Spring Security의 SecurityContext에 저장됨
        return new PrincipalDetails(user, oidcUser);
    }
}