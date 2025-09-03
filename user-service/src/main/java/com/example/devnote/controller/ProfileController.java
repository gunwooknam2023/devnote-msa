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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository userRepository;
    private final UserProfileService profileService;

    /** 특정 사용자의 프로필 정보 조회 */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponseDto<UserProfileDto>> getUserProfile(@PathVariable Long userId) {
        UserProfileDto profile = profileService.getUserProfile(userId);
        return ResponseEntity.ok(
                ApiResponseDto.<UserProfileDto>builder()
                        .message("User profile fetched successfully")
                        .statusCode(HttpStatus.OK.value())
                        .data(profile)
                        .build()
        );
    }

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

    /** 현재 로그인한 사용자의 회원 탈퇴를 처리 */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> withdraw() {
        profileService.withdrawCurrentUser();
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("회원 탈퇴가 성공적으로 처리되었습니다. 그동안 서비스를 이용해주셔서 감사합니다.")
                        .statusCode(200)
                        .build()
        );
    }

    /** 임시 토큰을 사용하여 탈퇴한 사용자의 정보를 조회 */
    @GetMapping("/withdrawn-info")
    public ResponseEntity<ApiResponseDto<WithdrawnUserDto>> getWithdrawnInfo(@RequestParam String token) {
        WithdrawnUserDto withdrawnInfoDto = profileService.getWithdrawnUserInfoByToken(token);
        return ResponseEntity.ok(
                ApiResponseDto.<WithdrawnUserDto>builder()
                        .message("Fetched withdrawn user info successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .data(withdrawnInfoDto)
                        .build()
        );
    }

    /**
     * 현재 로그인된 사용자의 프로필 사진을 변경
     * @param imageFile 새로 업로드할 이미지 파일 (multipart/form-data)
     */
    @PostMapping("/me/profile-image")
    public ResponseEntity<ApiResponseDto<ProfileImageResponseDto>> updateProfileImage(
            @RequestParam("image") MultipartFile imageFile) {

        ProfileImageResponseDto responseDto = profileService.updateProfileImage(imageFile);

        return ResponseEntity.ok(
                ApiResponseDto.<ProfileImageResponseDto>builder()
                        .message("프로필 사진이 성공적으로 변경되었습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(responseDto)
                        .build()
        );
    }

    /**
     * 현재 로그인된 사용자의 프로필 사진을 기본 이미지로 초기화
     */
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponseDto<ProfileImageResponseDto>> resetProfileImage() {

        ProfileImageResponseDto responseDto = profileService.resetProfileImage();

        return ResponseEntity.ok(
                ApiResponseDto.<ProfileImageResponseDto>builder()
                        .message("프로필 사진이 기본 이미지로 초기화되었습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(responseDto)
                        .build()
        );
    }
}