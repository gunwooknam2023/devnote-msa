package com.example.devnote.news_youtube_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka 로 보낼 콘텐츠 메시지 dto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentMessageDto {
    private String source;       // "NEWS" or "YOUTUBE"
    private String category;     // "BACKEND", "FRONTEND", "DEVOPS" 등 추가
    private String description;  // 설명 (News 전용)
    private String title;        // 제목
    private String link;         // 콘텐츠 원본 링크
    private String thumbnailUrl; // 썸네일 URL (YouTube 전용)
    private Instant publishedAt; // 게시일

    // 유튜브
    private String channelTitle;        // 유튜브 채널명
    private String channelThumbanilUrl; // 채널 썸네일
    private Long viewCount;             // 영상 조회수
}
