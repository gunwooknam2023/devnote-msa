package com.example.devnote.stats_service.dto;

import lombok.Data;

import java.time.Instant;

/**
 * processor-service로부터 콘텐츠 상세 정보를 받아오기 위한 DTO
 */
@Data
public class ContentDto {
    private Long id;
    private String source;       // "NEWS" or "YOUTUBE"
    private String category;     // "BACKEND", "FRONTEND", "DEVOPS" 등 추가
    private String channelId;    // 유튜브 채널 ID
    private String title;        // 제목
    private String description;  // 설명 (News 전용)
    private String link;         // 콘텐츠 원본 링크
    private String thumbnailUrl; // 썸네일 URL (YouTube 전용)
    private Instant publishedAt; // 게시일
    private Instant createdAt;   // 생성일

    // 유튜브
    private String channelTitle;        // 유튜브 채널명
    private String channelThumbnailUrl; // 채널 썸네일
    private Long viewCount;             // 영상 조회수
    private Long localViewCount;        // 로컬 영상 조회수
    private Long durationSeconds;       // 영상 길이(초)
    private String videoForm;           // "SHORTS" or "LONGFORM"
    private Long subscriberCount;       // 구독자 수
}
