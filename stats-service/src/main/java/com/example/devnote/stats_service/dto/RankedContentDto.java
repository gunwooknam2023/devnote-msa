package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 콘텐츠 랭킹 정보를 최종적으로 반환하기 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedContentDto {
    private long rank;              // 순위
    private long count;             // 집계 수 (찜 또는 댓글)
    private Long id;
    private String source;
    private String category;
    private String title;
    private String link;
    private String thumbnailUrl;
    private Instant publishedAt;
    private String channelTitle;
    private String channelId;
    private Long viewCount;
    private String description;
    private Instant createdAt;
    private String channelThumbnailUrl;
    private Long localViewCount;
    private Long durationSeconds;
    private String videoForm;
    private Long subscriberCount;
}