package com.example.devnote.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 문의사항 목록 조회 API 응답 DTO
 */
@Data
@Builder
public class InquiryListResponseDto {
    private Long id;
    private String title; // 비공개글일 경우 "비공개 게시글입니다."로 대체됨
    private Long userId;
    private String username;
    private String userPicture;
    private boolean answered;
    private boolean isPublic;
    private Instant createdAt;
}