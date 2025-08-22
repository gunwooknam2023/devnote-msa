package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 찜한 영상/뉴스 반환 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDto {
    /** 콘텐츠 고유 ID (processor-service 기준) */
    private Long id;

    /** "NEWS" 또는 "YOUTUBE" */
    private String source;

    /** 분류(예: BACKEND, FRONTEND 등) */
    private String category;

    /** 채널 ID */
    private String channelId;

    /** 제목 */
    private String title;

    /** 설명(News 전용) */
    private String description;

    /** 원본 콘텐츠 링크 */
    private String link;

    /** 썸네일 URL(YouTube 전용) */
    private String thumbnailUrl;

    /** 게시일 */
    private Instant publishedAt;

    /** DB에 저장된 생성일 */
    private Instant createdAt;

    // YouTube 전용
    /** 채널명 */
    private String channelTitle;

    /** 채널 썸네일 URL */
    private String channelThumbnailUrl;

    /** 조회수 */
    private Long viewCount;

    /** 재생 길이(초) */
    private Long durationSeconds;

    /** "SHORTS" 또는 "LONGFORM" */
    private String videoForm;

    /** 구독자 수 */
    private Long subscriberCount;

    /** 찜 수 */
    private Long favoriteCount;

    /** 댓글 수 */
    private Long commentCount;

    /** 로컬 영상 조회수 */
    private Long localViewCount;
}