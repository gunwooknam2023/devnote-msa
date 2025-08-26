package com.example.devnote.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 문의사항 상세 조회 API 응답 DTO
 */
@Data
@Builder
public class InquiryDetailResponseDto {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String username;
    private String userPicture;
    private boolean answered;
    private boolean isPublic;
    private List<String> imageUrls;
    private Instant createdAt;
    private Instant updatedAt;
}