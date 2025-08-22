package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채널의 통계(찜) 변경을 알리기 위한 Kafka 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelStatsUpdateDto {
    private Long channelSubscriptionId;
    private int favoriteDelta; // 찜 증감 (+1 또는 -1)
}
