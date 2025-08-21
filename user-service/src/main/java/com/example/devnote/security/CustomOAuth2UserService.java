package com.example.devnote.security;

import com.example.devnote.entity.User;
import com.example.devnote.entity.WithdrawnUser;
import com.example.devnote.repository.UserRepository;
import com.example.devnote.repository.WithdrawnUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * OAuth2 로그인 후 사용자 정보 저장·업데이트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepo;
    private final WithdrawnUserRepository withdrawnUserRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User user = super.loadUser(req);
        var info = OAuth2UserInfoFactory.get(
                req.getClientRegistration().getRegistrationId(),
                user.getAttributes()
        );

        // 재가입 방지 로직
        Optional<WithdrawnUser> withdrawnUserOpt = withdrawnUserRepo.findByEmail(info.getEmail());
        if (withdrawnUserOpt.isPresent()) {
            WithdrawnUser withdrawnUser = withdrawnUserOpt.get();
            // 재가입 가능 시간이 현재보다 미래인 경우 (아직 7일이 지나지 않음)
            if (withdrawnUser.getCanRejoinAt().isAfter(Instant.now())) {
                log.warn("탈퇴 후 7일이 경과하지 않은 사용자의 재가입 시도: {}", info.getEmail());
                // OAuth2AuthenticationFailureHandler가 처리할 수 있도록 예외 발생
                throw new OAuth2AuthenticationException("탈퇴한 계정입니다. 7일 후에 다시 가입해주세요.");
            } else {
                // 7일이 지났다면, 탈퇴 기록은 삭제하여 재가입을 허용
                withdrawnUserRepo.delete(withdrawnUser);
                log.info("재가입 방지 기간이 만료되어 탈퇴 기록 삭제: {}", info.getEmail());
            }
        }

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