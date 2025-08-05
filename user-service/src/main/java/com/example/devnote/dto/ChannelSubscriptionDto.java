package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 찜한 채널 반환 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelSubscriptionDto {
    /** ChannelSubscription.id */
    private Long id;

    /** 유튜브 채널명 */
    private String youtubeName;

    /** 유튜브 채널 ID */
    private String channelId;

    /** 채널 썸네일 URL */
    private String channelThumbnailUrl;

    /** 초기 전체 로딩 완료 여부 */
    private boolean initialLoaded;

    /** 구독자 수 */
    private Long subscriberCount;
}