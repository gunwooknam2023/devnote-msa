package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponseDto {
    private Long id;
    private String provider;
    private String providerId;
    private String email;
    private String name;
    private String imageUrl;
}
