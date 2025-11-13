package com.example.devnote.news_youtube_service.dto;

import lombok.*;

/**
 * 전체 채널 구독 통계 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelSubscriptionSummaryDto {

    private String channelId;
    private String channelThumbnail;
    private String channelName;
    private String source;
    private Long subscriberCount;
    private Long favoriteCount;
}
