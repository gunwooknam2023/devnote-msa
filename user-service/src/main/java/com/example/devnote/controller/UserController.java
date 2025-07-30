package com.example.devnote.controller;

import com.example.devnote.dto.LoginResponseDto;
import com.example.devnote.dto.TokenRefreshRequestDto;
import com.example.devnote.dto.UserProfileResponseDto;
import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import com.example.devnote.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    /**
     * 리프레시 토큰으로 액세스/리프레시 토큰 재발급
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(
            @Valid @RequestBody TokenRefreshRequestDto req
    ) {
        String refreshToken = req.getRefreshToken();
        if (!tokenService.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = tokenService.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        if (!refreshToken.equals(user.getRefreshToken())) {
            return ResponseEntity.status(403).build();
        }

        String newAccess = tokenService.generateAccessToken(user);
        String newRefresh = tokenService.generateRefreshToken(user);
        return ResponseEntity.ok(new LoginResponseDto(newAccess, newRefresh));
    }

    /**
     * 내 프로필 조회
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal User currentUser
    ) {
        UserProfileResponseDto dto = new UserProfileResponseDto(
                currentUser.getId(),
                currentUser.getProvider(),
                currentUser.getProviderId(),
                currentUser.getEmail(),
                currentUser.getName(),
                currentUser.getImageUrl()
        );
        return ResponseEntity.ok(dto);
    }
}
