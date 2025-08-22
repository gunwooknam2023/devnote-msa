package com.example.devnote.news_youtube_service.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 채널 정보 엔티티
 */
@Entity
@Table(name = "channel_subscription")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 채널명 (유튜브 채널명 또는 언론사명) */
    @Column(nullable = false, length = 100)
    private String youtubeName;

    /** 채널 ID */
    @Column(nullable = false, unique = true, length = 50)
    private String channelId;

    /** 채널 썸네일 (유튜브 채널 썸네일 또는 언론사 로고) */
    @Column(length = 512)
    private String channelThumbnailUrl;

    /** 구독자 수 (뉴스의 경우 null) */
    @Column(name = "subscriber_count")
    private Long subscriberCount;

    /** 초기 전체 로딩 완료 여부 (뉴스의 경우 true) */
    @Column(nullable = false)
    private boolean initialLoaded;

    /** 소스 구분 ("YOUTUBE" 또는 "NEWS") */
    @Column(length = 20)
    private String source;

    /** 찜 수 */
    @Column(name = "favorite_count", nullable = false)
    @Builder.Default
    private Long favoriteCount = 0L;
}
