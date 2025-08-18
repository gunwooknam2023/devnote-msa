package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채널 랭킹 정보를 최종적으로 반환하기 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedChannelDto {
    private long rank;                  // 순위
    private long count;                 // 찜 수
    private Long id;
    private String youtubeName;
    private String channelId;
    private String channelThumbnailUrl;
    private Long subscriberCount;
}