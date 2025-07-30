package com.example.devnote.security;

import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * OAuth2 로그인 후 사용자 정보 저장·업데이트
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User user = super.loadUser(req);
        var info = OAuth2UserInfoFactory.get(
                req.getClientRegistration().getRegistrationId(),
                user.getAttributes()
        );

        User entity = userRepo.findByProviderAndProviderId(
                req.getClientRegistration().getRegistrationId(), info.getId()
        ).orElseGet(() -> User.builder()
                .provider(req.getClientRegistration().getRegistrationId())
                .providerId(info.getId())
                .email(info.getEmail())
                .name(info.getName())
                .picture(info.getImageUrl())
                .build()
        );

        // 최신 정보로 갱신
        entity.setName(info.getName());
        entity.setPicture(info.getImageUrl());
        userRepo.save(entity);

        return user;
    }
}