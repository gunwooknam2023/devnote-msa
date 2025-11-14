package com.example.devnote.news_youtube_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 채널 추가 요청 엔티티
 */
@Entity
@Table(name = "channel_add_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelAddRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 요청한 채널명 (유튜브 채널명 또는 언론사명) */
    @Column(nullable = false, length = 100)
    private String channelName;

    /** 채널 링크 (유튜브 채널 URL 또는 뉴스 언론사 URL, 선택사항) */
    @Column(length = 512)
    private String link;

    /** 소스 구분 ("YOUTUBE" 또는 "NEWS") */
    @Column(nullable = false, length = 20)
    private String source;

    /** 채널 추가 요청 사유 (선택사항) */
    @Column(columnDefinition = "TEXT")
    private String requestReason;

    /** 요청 일시 */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
