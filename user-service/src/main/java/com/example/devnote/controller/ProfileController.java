package com.example.devnote.controller;

import com.example.devnote.dto.ProfileRequestDto;
import com.example.devnote.dto.ProfileResponseDto;
import com.example.devnote.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserProfileService profileService;

    /** 자기소개 수정 */
    @PutMapping
    public ResponseEntity<ProfileResponseDto> updateProfile(
            @Validated @RequestBody ProfileRequestDto req) {
        String intro = profileService.update(req);
        return ResponseEntity.ok(new ProfileResponseDto(intro));
    }
}