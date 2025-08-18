package com.example.devnote.stats_service.dto.internal;

import lombok.Data;

/**
 * user-service로부터 채널 랭킹 ID 목록을 받아오기 위한 DTO
 */
@Data
public class RankedChannelIdDto {
    private Long channelId;
    private long count;
}
