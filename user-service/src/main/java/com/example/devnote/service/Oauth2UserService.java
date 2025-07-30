package com.example.devnote.service;

import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class Oauth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Map<String, Object> attr = oauth.getAttributes();

        // provider 별 키 추출
        String providerId;
        String email;
        String name;
        String imageUrl;

        switch (provider) {
            case "GOOGLE":
                providerId = (String) attr.get("sub");
                email      = (String) attr.get("email");
                name       = (String) attr.get("name");
                imageUrl   = (String) attr.get("picture");
                break;
            case "NAVER":
                Map<?,?> resp = (Map<?,?>) attr.get("response");
                providerId = resp.get("id").toString();
                email      = (String) resp.get("email");
                name       = (String) resp.get("name");
                imageUrl   = (String) resp.get("profile_image");
                break;
            case "KAKAO":
                providerId = attr.get("id").toString();
                Map<?,?> kakaoProf = (Map<?,?>) attr.get("properties");
                Map<?,?> kakaoAccount = (Map<?,?>) attr.get("kakao_account");
                email      = (String) kakaoAccount.get("email");
                name       = (String) kakaoProf.get("nickname");
                imageUrl   = (String) kakaoProf.get("profile_image");
                break;
            default:
                throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + provider);
        }

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(provider)
                                .providerId(providerId)
                                .email(email)
                                .name(name)
                                .imageUrl(imageUrl)
                                .build()
                ));

        // Spring Security 의 인증 객체로 사용하기 위해
        return new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                Map.of(
                        "id", user.getId(),
                        "provider", provider,
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "imageUrl", user.getImageUrl()
                ),
                "id"
        );
    }
}
