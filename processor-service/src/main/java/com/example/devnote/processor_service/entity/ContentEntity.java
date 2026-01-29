package com.example.devnote.processor_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "contents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String category;
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ContentStatus status = ContentStatus.ACTIVE;

    @Column(name = "channel_id", length = 50)
    private String channelId;

    @Column(length = 500)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String channelTitle;

    @Column(length = 512)
    private String channelThumbnailUrl;

    @Column(length = 20)
    private String videoForm;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "local_view_count", nullable = false)
    @Builder.Default
    private Long localViewCount = 0L;

    @Column(name = "favorite_count", nullable = false)
    @Builder.Default
    private Long favoriteCount = 0L;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Long commentCount = 0L;

    private Long viewCount;
    private Long durationSeconds;
    private String thumbnailUrl;
    private Instant publishedAt;
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
