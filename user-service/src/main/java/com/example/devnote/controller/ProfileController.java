package com.example.devnote.controller;

import com.example.devnote.dto.*;
import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import com.example.devnote.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository userRepository;
    private final UserProfileService profileService;

    /** 프로필 페이지 정보 반환 (찜 영상, 뉴스, 채널 / 작성한 댓글 목록) */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDto<DashboardDto>> getDashboard() {
        DashboardDto dto = profileService.getDashboard();

        return ResponseEntity.ok(
                ApiResponseDto.<DashboardDto> builder()
                        .message("Fetched user dashboard")
                        .statusCode(200)
                        .data(dto)
                        .build()
        );
    }

    /** 자기소개 수정 */
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponseDto<ProfileResponseDto>> updateProfile(
            @Validated @RequestBody ProfileRequestDto req) {
        String intro = profileService.update(req);
        return ResponseEntity.ok(
                ApiResponseDto.<ProfileResponseDto>builder()
                        .message("Profile updated")
                        .statusCode(200)
                        .data(new ProfileResponseDto(intro))
                        .build()
        );
    }

    /** 로그인 인식용 현재 사용자 정보 반환 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserInfoDto>> getCurrentUser(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        UserInfoDto dto = new UserInfoDto(user.getId(), user.getName(), user.getPicture(), user.getSelfIntroduction());
        return ResponseEntity.ok(
                ApiResponseDto.<UserInfoDto>builder()
                        .message("Fetched user")
                        .statusCode(200)
                        .data(dto)
                        .build()
        );
    }
}