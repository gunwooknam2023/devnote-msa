package com.example.devnote.security;

import com.example.devnote.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Security의 인증 주체(Principal)로 사용될 커스텀 클래스
 */
@Getter
public class PrincipalDetails implements OAuth2User, OidcUser {

    private final User user;
    private final OAuth2User oauth2User; // 원본 OAuth2User 또는 OidcUser 저장

    public PrincipalDetails(User user, OAuth2User oauth2User) {
        this.user = user;
        this.oauth2User = oauth2User;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return user.getProviderId();
    }

    @Override
    public Map<String, Object> getClaims() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getClaims();
        }
        return null;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getUserInfo();
        }
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getIdToken();
        }
        return null;
    }
}