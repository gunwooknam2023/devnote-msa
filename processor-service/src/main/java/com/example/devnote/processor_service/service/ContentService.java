package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.dto.ContentDto;
import com.example.devnote.processor_service.dto.ContentMessageDto;
import com.example.devnote.processor_service.dto.PageResponseDto;
import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 비즈니스 로직 수행용 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private final ContentRepository contentRepository;
    private final RedisTemplate<String, Object> redis;

    private static final int DEFAULT_LIMIT = 20;
    private static final String CACHE_PREFIX = "cache:";

    /** Kafka 메시지 수신 → 저장 + Redis 캐시 */
    @KafkaListener(
            topics = "raw.content",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ContentMessageDto msg) {
        log.info("Received Kafka msg: {} / {}", msg.getCategory(), msg.getTitle());
        try {
            // 중복 체크
            boolean exists = contentRepository
                    .findBySourceAndLink(msg.getSource(), msg.getLink())
                    .isPresent();

            if (exists) {
                log.debug("Duplicate skipped: {}", msg.getLink());
                return;
            }

            // 엔티티 변환 & 저장
            ContentEntity ent = ContentEntity.builder()
                    .source(msg.getSource())
                    .category(msg.getCategory())
                    .title(msg.getTitle())
                    .link(msg.getLink())
                    .thumbnailUrl(msg.getThumbnailUrl())
                    .description(msg.getDescription())
                    .publishedAt(msg.getPublishedAt())
                    .build();
            ent = contentRepository.save(ent);
            log.info("Saved ContentEntity id={}", ent.getId());

            // Redis 캐시 갱신 (최신 100개)
            String key = CACHE_PREFIX + msg.getCategory();
            redis.opsForList().leftPush(key, toDto(ent));
            redis.opsForList().trim(key, 0, 99);

        } catch (Exception ex) {
            log.error("Error processing Kafka msg: {}", msg, ex);
        }
    }

    /** 커서 기반 조회: category, cursor(id 이하), limit */
    public PageResponseDto<ContentDto> getContents(
            String source,
            String category,
            Long cursor,
            Integer limit
    ) {
        int fetchLimit = (limit == null ? DEFAULT_LIMIT : limit);
        Pageable pg = PageRequest.of(0, fetchLimit, Sort.by("id").descending());

        List<ContentEntity> ents;
        if (category == null || category.isBlank()) {
            if (cursor == null || cursor <= 0) {
                ents = contentRepository.findBySourceOrderByIdDesc(source, pg);
            } else {
                ents = contentRepository.findBySourceAndIdLessThanOrderByIdDesc(source, cursor, pg);
            }
        } else {
            // 기존 카테고리 조회
            if (cursor == null || cursor <= 0) {
                ents = contentRepository.findByCategoryOrderByIdDesc(category, pg);
            } else {
                ents = contentRepository.findByCategoryAndIdLessThanOrderByIdDesc(category, cursor, pg);
            }
        }

        List<ContentDto> dtos = ents.stream().map(this::toDto).collect(Collectors.toList());
        Long nextCursor = dtos.isEmpty() ? null : dtos.get(dtos.size() - 1).getId();

        return PageResponseDto.<ContentDto>builder()
                .items(dtos)
                .nextCursor(nextCursor)
                .build();
    }

    private ContentDto toDto(ContentEntity e) {
        return ContentDto.builder()
                .id(e.getId())
                .source(e.getSource())
                .category(e.getCategory())
                .title(e.getTitle())
                .link(e.getLink())
                .thumbnailUrl(e.getThumbnailUrl())
                .description(e.getDescription())
                .publishedAt(e.getPublishedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
