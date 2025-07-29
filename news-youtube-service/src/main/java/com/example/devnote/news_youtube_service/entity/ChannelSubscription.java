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

    /** 채널명 */
    @Column(nullable = false, length = 100)
    private String youtubeName;

    /** 채널 ID */
    @Column(nullable = false, unique = true, length = 50)
    private String channelId;

    /** 채널 썸네일 */
    @Column(length = 512)
    private String channelThumbnailUrl;

    /** 초기 전체 로딩 완료 여부 */
    @Column(nullable = false)
    private boolean initialLoaded;
}
