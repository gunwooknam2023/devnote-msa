package com.example.devnote.security;

import com.example.devnote.entity.User;
import com.example.devnote.exception.UserWithdrawnException;
import com.example.devnote.repository.UserRepository;
import com.example.devnote.repository.WithdrawnUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserProcessor {

    private final UserRepository userRepo;
    private final WithdrawnUserRepository withdrawnUserRepo;

    @Value("${app.default-profile-image-url:/images/default-avatar.png}")
    private String defaultProfileImageUrl;

    @Transactional
    public User processUser(String registrationId, OAuth2User oauth2User) {
        var info = OAuth2UserInfoFactory.get(registrationId, oauth2User.getAttributes());

        // 재가입 방지 로직
        withdrawnUserRepo.findByEmail(info.getEmail()).ifPresent(withdrawnUser -> {
            if (withdrawnUser.getCanRejoinAt().isAfter(Instant.now())) {
                log.warn("탈퇴 후 7일이 경과하지 않은 사용자의 로그인 시도: {}", info.getEmail());
                throw new UserWithdrawnException(info.getEmail());
            } else {
                withdrawnUserRepo.delete(withdrawnUser);
                log.info("재가입 방지 기간이 만료되어 탈퇴 기록 삭제: {}", info.getEmail());
            }
        });

        // 제공자 정보와 제공자 내부의 고유 ID로 사용자를 조회
        String providerId = info.getId();
        String provider = registrationId;

        User entity = userRepo.findByProviderAndProviderId(provider, providerId)
                // 이미 가입된 사용자인 경우: 이름(닉네임) 정보 업데이트
                .map(existingUser -> {
                    existingUser.setName(info.getName());
                    return existingUser;
                })
                // 새로 가입하는 사용자인 경우: User 엔티티를 생성하여 DB에 저장
                .orElseGet(() -> {
                    return User.builder()
                            .provider(provider)
                            .providerId(providerId)
                            .email(info.getEmail())
                            .name(info.getName())
                            .picture(defaultProfileImageUrl)
                            .build();
                });

        return userRepo.save(entity);
    }
}