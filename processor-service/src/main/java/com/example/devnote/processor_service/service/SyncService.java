package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.es.EsContentRepository;
import com.example.devnote.processor_service.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final ContentRepository contentRepository;
    private final EsContentRepository esContentRepository;

    /**
     * 매 시간마다 최근 1시간 동안 생성/수정된 데이터를 자동으로 동기화
     * 매 정시 0분 0초에 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleHourlySync() {
        log.info("[SCHEDULED] Starting hourly data reconciliation.");

        // 현재 시간 기준으로 1시간 전 데이터에 대해 동기화 실행
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        Instant now = Instant.now();

        Specification<ContentEntity> spec = (root, query, cb) ->
                cb.between(root.get("createdAt"), oneHourAgo, now);

        List<ContentEntity> recentContents = contentRepository.findAll(spec);

        if (recentContents.isEmpty()) {
            log.info("[SCHEDULED] No recent data to sync.");
            return;
        }

        List<EsContent> esDocuments = recentContents.stream()
                .map(this::toEsContent)
                .toList();

        esContentRepository.saveAll(esDocuments);

        log.info("[SCHEDULED] Automatically reconciled {} documents.", esDocuments.size());
    }

    /**
     * 지정된 날짜 범위의 데이터를 MariaDB에서 읽어 Elasticsearch에 재색인
     * @param start 시작일
     * @param end   종료일
     * @return 동기화된 문서의 개수
     */
    @Transactional(readOnly = true)
    public long syncContentsByDateRange(LocalDate start, LocalDate end) {
        log.info("Starting data sync from {} to {}", start, end);

        // 1. 기간에 해당하는 데이터를 MariaDB에서 조회
        Instant startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = end.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        Specification<ContentEntity> spec = (root, query, cb) ->
                cb.between(root.get("createdAt"), startInstant, endInstant);

        List<ContentEntity> contentsFromDb = contentRepository.findAll(spec);

        if (contentsFromDb.isEmpty()) {
            log.info("No data to sync in the given period.");
            return 0;
        }

        // 2. 조회된 데이터를 Elasticsearch 문서 형식으로 변환
        List<EsContent> esDocuments = contentsFromDb.stream()
                .map(this::toEsContent)
                .toList();

        // 3. Elasticsearch에 일괄 저장(saveAll)
        esContentRepository.saveAll(esDocuments);

        log.info("Successfully synced {} documents to Elasticsearch.", esDocuments.size());
        return esDocuments.size();
    }

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