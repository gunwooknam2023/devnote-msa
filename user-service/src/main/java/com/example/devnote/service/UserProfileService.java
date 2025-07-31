package com.example.devnote.service;

import com.example.devnote.dto.ProfileRequestDto;
import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepo;

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /** 자기소개 수정 */
    @Transactional
    public String update(ProfileRequestDto req) {
        User me = currentUser();
        me.setSelfIntroduction(req.getSelfIntroduction());
        userRepo.save(me);
        return me.getSelfIntroduction();
    }
}
