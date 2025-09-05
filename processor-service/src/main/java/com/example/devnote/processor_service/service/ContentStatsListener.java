package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.dto.ContentStatsUpdateDto;
import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.es.EsContentRepository;
import com.example.devnote.processor_service.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentStatsListener {

    private final ContentRepository contentRepository;
    private final EsContentRepository esContentRepository;

    /**
     * 'content-stats-update' 토픽을 구독하여 찜/댓글 수를 DB에 반영
     */
    @KafkaListener(
            topics = "content-stats-update",
            containerFactory = "statsUpdateContainerFactory"
    )
    @Transactional
    public void listen(ContentStatsUpdateDto message) {
        log.info("Received stats update message: {}", message);
        contentRepository.findById(message.getContentId()).ifPresent(content -> {
            // DB 업데이트
            content.setFavoriteCount(content.getFavoriteCount() + message.getFavoriteDelta());
            content.setCommentCount(content.getCommentCount() + message.getCommentDelta());
            contentRepository.save(content);

            // Elasticsearch 업데이트(재색인)
            esContentRepository.save(toEsContent(content));
            log.info("Updated EsContent stats for id={}", content.getId());
        });
    }

    /** ContentEntity를 EsContent로 변환하는 메서드 */
    private EsContent toEsContent(ContentEntity e) {
        return EsContent.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .channelTitle(e.getChannelTitle())
                .category(e.getCategory())
                .source(e.getSource())
                .channelId(e.getChannelId())
                .link(e.getLink())
                .channelThumbnailUrl(e.getChannelThumbnailUrl())
                .videoForm(e.getVideoForm())
                .thumbnailUrl(e.getThumbnailUrl())
                .publishedAt(e.getPublishedAt())
                .createdAt(e.getCreatedAt())
                .viewCount(e.getViewCount())
                .localViewCount(e.getLocalViewCount())
                .durationSeconds(e.getDurationSeconds())
                .subscriberCount(e.getSubscriberCount())
                .favoriteCount(e.getFavoriteCount())
                .commentCount(e.getCommentCount())
                .build();
    }
}
