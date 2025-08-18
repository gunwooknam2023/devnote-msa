package com.example.devnote.stats_service.dto;

import lombok.Data;

/**
 * news-youtube-service로부터 채널 상세 정보를 받아오기 위한 DTO
 */
@Data
public class ChannelSubscriptionDto {
    private Long id;
    private String youtubeName;
    private String channelId;
    private String channelThumbnailUrl;
    private Long subscriberCount;
}
