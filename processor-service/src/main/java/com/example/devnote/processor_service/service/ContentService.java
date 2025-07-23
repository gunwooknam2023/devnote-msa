package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.dto.ContentDto;
import com.example.devnote.processor_service.dto.ContentMessageDto;
import com.example.devnote.processor_service.dto.PageResponseDto;
import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

/**
 * 비즈니스 로직 수행용 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private final ContentRepository contentRepository;
    private final RedisTemplate<String, Object> redis;
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

    /**
     * 페이지네이션 + 필터 + 정렬 적용된 콘텐츠 조회
     */
    public PageResponseDto<ContentDto> getContents(
            int page,
            int size,
            String source,
            String category,
            String title,
            String sortOrder
    ) {
        // Sort 설정 (publishedAt 기준)
        Sort sort = Sort.by("publishedAt");

        if("newest".equalsIgnoreCase(sortOrder)) {
            sort = sort.descending();
        } else if ("oldest".equalsIgnoreCase(sortOrder)) {
            sort = sort.ascending();
        } else {
            log.warn("Unkown sort order '{}', defaulting to newst", sortOrder);
            sort = sort.descending();
        }

        // Pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // 동적 필터링
        Specification<ContentEntity> spec = buildSpecification(source, category, title);

        // 조회
        Page<ContentEntity> entityPage = contentRepository.findAll(spec, pageable);

        // Dto 변환
        List<ContentDto> dtos = entityPage.stream()
                .map(this::toDto)
                .toList();

        // PageResponseDto 래핑
        return PageResponseDto.<ContentDto>builder()
                .items(dtos)
                .page(entityPage.getNumber())
                .size(entityPage.getSize())
                .totalElements(entityPage.getTotalElements())
                .totalPages(entityPage.getTotalPages())
                .build();
    }

    /**
     * 조건에 따른 JPA Specification 생성
     */
    private Specification<ContentEntity> buildSpecification(
            String source,
            String category,
            String title
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Entity → DTO 변환
     */
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
