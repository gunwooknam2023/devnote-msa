package com.example.devnote.news_youtube_service.dto;

import lombok.Data;

/**
 * user-service로부터 채널 통계 변경을 수신하기 위한 Kafka 메시지 DTO
 */
@Data
public class ChannelStatsUpdateDto {
    private Long channelSubscriptionId;
    private int favoriteDelta;
}
