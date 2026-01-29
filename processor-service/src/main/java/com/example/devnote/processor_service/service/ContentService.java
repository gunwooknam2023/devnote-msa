package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.dto.CategoryCountDto;
import com.example.devnote.processor_service.dto.ContentDto;
import com.example.devnote.processor_service.dto.ContentMessageDto;
import com.example.devnote.processor_service.dto.PageResponseDto;
import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.entity.ContentStatus;
import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.es.EsContentRepository;
import com.example.devnote.processor_service.repository.ContentRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
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
    private final StringRedisTemplate sredis;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EsContentRepository esContentRepository;

    private static final String CACHE_PREFIX = "cache:";
    private static final String VIEW_KEY_FMT = "views:content:%d:count";
    private static final String DEDUP_KEY_FMT = "views:dedup:%d:%s";

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
                    .channelId(msg.getChannelId())
                    .title(msg.getTitle())
                    .link(msg.getLink())
                    .thumbnailUrl(msg.getThumbnailUrl())
                    .description(msg.getDescription())
                    .publishedAt(msg.getPublishedAt())
                    .channelTitle(msg.getChannelTitle())
                    .channelThumbnailUrl(msg.getChannelThumbnailUrl())
                    .viewCount(msg.getViewCount())
                    .durationSeconds(msg.getDurationSeconds())
                    .videoForm(msg.getVideoForm())
                    .subscriberCount(msg.getSubscriberCount())
                    .localViewCount(0L)
                    .favoriteCount(0L)
                    .commentCount(0L)
                    .build();
            ent = contentRepository.save(ent);
            log.info("Saved ContentEntity id={}", ent.getId());

            // MariaDB 저장 성공 후, Elasticsearch에도 색인
            esContentRepository.save(toEsContent(ent));
            log.info("Indexed EsContent id={}", ent.getId());


            // 신규 콘텐츠 생성 이벤트 발행
            kafkaTemplate.send("content.created", String.valueOf(ent.getId()));

            // Redis 캐시 갱신 (최신 100개)
            String key = CACHE_PREFIX + msg.getCategory();
            redis.opsForList().leftPush(key, toDto(ent));
            redis.opsForList().trim(key, 0, 99);

        } catch (Exception ex) {
            log.error("Error processing Kafka msg: {}", msg, ex);
        }
    }

    /**
     * 페이지네이션 + 필터 + 정렬 적용된 콘텐츠 조회 (모든 정렬을 DB에서 처리)
     */
    public PageResponseDto<ContentDto> getContents(
            int page, int size, String source, String category, String channelId, String channelTitle, String title, String sortOrder
    ) {
        // 모든 정렬 조건을 DB에서 처리하도록 Sort 객체 생성
        Sort sort = buildSort(sortOrder);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Specification으로 동적 필터링 조건 생성
        Specification<ContentEntity> spec = buildSpecification(source, category, channelId, channelTitle, title);

        // DB에서 필터링 및 정렬된 데이터 조회
        Page<ContentEntity> entityPage = contentRepository.findAll(spec, pageable);

        // DTO로 변환하여 최종 응답 반환
        List<ContentDto> dtos = entityPage.stream().map(this::toDto).toList();
        return new PageResponseDto<>(dtos, entityPage.getNumber(), entityPage.getSize(), entityPage.getTotalElements(), entityPage.getTotalPages());
    }

    /**
     * sortOrder 문자열에 따라 Sort 객체를 생성하는 헬퍼 메서드
     */
    private Sort buildSort(String sortOrder) {
        return switch (sortOrder.toLowerCase()) {
            case "oldest" -> Sort.by("publishedAt").ascending();
            case "views_desc" -> Sort.by("localViewCount").descending();
            case "views_asc" -> Sort.by("localViewCount").ascending();
            case "youtube_views_desc" -> Sort.by("viewCount").descending();
            case "youtube_views_asc" -> Sort.by("viewCount").ascending();
            case "favorites_desc" -> Sort.by("favoriteCount").descending();
            case "comments_desc" -> Sort.by("commentCount").descending();
            default -> Sort.by("publishedAt").descending();
        };
    }


    /**
     * 조건에 따른 JPA Specification 생성
     * - ACTIVE 상태인 콘텐츠만 조회
     */
    private Specification<ContentEntity> buildSpecification(
            String source,
            String category,
            String channelId,
            String channelTitle,
            String title
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), ContentStatus.ACTIVE));

            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (channelId != null && !channelId.isBlank()) {
                predicates.add(cb.equal(root.get("channelId"), channelId));
            }
            if (channelTitle != null && !channelTitle.isBlank()) {
                predicates.add(cb.equal(root.get("channelTitle"), channelTitle));
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
                .channelId(e.getChannelId())
                .title(e.getTitle())
                .link(e.getLink())
                .thumbnailUrl(e.getThumbnailUrl())
                .description(e.getDescription())
                .publishedAt(e.getPublishedAt())
                .channelTitle(e.getChannelTitle())
                .channelThumbnailUrl(e.getChannelThumbnailUrl())
                .viewCount(e.getViewCount())
                .localViewCount(e.getLocalViewCount())
                .durationSeconds(e.getDurationSeconds())
                .videoForm(e.getVideoForm())
                .subscriberCount(e.getSubscriberCount())
                .favoriteCount(e.getFavoriteCount())
                .commentCount(e.getCommentCount())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public ContentDto getContentById(Long id) {
        return contentRepository.findById(id)
                .filter(entity -> entity.getStatus() != ContentStatus.HIDDEN) // HIDDEN이면 없는 취급
                .map(this::toDto)
                .orElseThrow(() -> {
                    log.warn("Content not found or hidden: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Content not found: " + id
                    );
                });
    }

    /**
     * 콘텐츠 존재 여부 (반환 타입 void)
     */
    public void verifyExists(Long id) {
        boolean exists = contentRepository.existsById(id);
        if (!exists) {
            log.warn("Attempt to delete non-existent content: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Content not found: " + id
            );
        }
    }

    /**
     * 콘텐츠 삭제 + ES 문서 삭제
     */
    public void deleteById(Long id) {
        contentRepository.deleteById(id);
        esContentRepository.deleteById(id);
        log.info("Content deleted from DB and ES: {}", id);
    }

    /**
     * 삭제/비공개 감지된 콘텐츠를 HIDDEN 상태로 변경
     * (썸네일 120x90 감지 시 프론트에서 호출)
     */
    @Transactional
    public boolean hideContent(Long id) {
        return contentRepository.findById(id)
                .map(entity -> {
                    // 이미 HIDDEN이면 스킵
                    if (entity.getStatus() == ContentStatus.HIDDEN) {
                        log.debug("Content already hidden: {}", id);
                        return false;
                    }
                    
                    entity.setStatus(ContentStatus.HIDDEN);
                    contentRepository.save(entity);
                    
                    // ES에서도 삭제
                    esContentRepository.deleteById(id);
                    
                    log.info("Content hidden due to deleted/private video: {}", id);
                    return true;
                })
                .orElse(false);
    }

    /**
     * DB에서 HIDDEN 상태인 모든 콘텐츠를 ES에서 삭제 (동기화)
     * - 배포 후 한 번 실행하여 기존 HIDDEN 데이터 정리
     */
    @Transactional(readOnly = true)
    public int syncHiddenContentToEs() {
        List<ContentEntity> hiddenContents = contentRepository.findByStatus(ContentStatus.HIDDEN);
        int count = 0;
        
        for (ContentEntity entity : hiddenContents) {
            try {
                esContentRepository.deleteById(entity.getId());
                count++;
                log.debug("Deleted hidden content from ES: {}", entity.getId());
            } catch (Exception e) {
                log.warn("Failed to delete hidden content from ES: {}", entity.getId(), e);
            }
        }
        
        log.info("Synced {} hidden contents to ES (deleted)", count);
        return count;
    }

    /**
     * 조회수 카운팅 (뉴스/유튜브 공용)
     */
    public void countView(Long id, @Nullable HttpServletRequest req) {
        if (!contentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found: " + id);
        }

        // IP+UA 기반 중복 방지 (10분간)
        if (req != null) {
            String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                    .map(x -> x.split(",")[0].trim())
                    .orElseGet(() -> req.getRemoteAddr());
            String ua = Optional.ofNullable(req.getHeader("User-Agent")).orElse("-");
            String dedupKey = DEDUP_KEY_FMT.formatted(id, Integer.toHexString(Objects.hash(ip, ua)));
            Boolean first = sredis.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofMinutes(10));

            if (Boolean.FALSE.equals(first)) return;
        }

        // 합계 카운터 증가
        String key = VIEW_KEY_FMT.formatted(id);
        sredis.opsForValue().increment(key);
    }

    /**
     * 조회수 1분마다 Redis -> DB 반영
     */
    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void flushViewCounts() {
        // 운영 모드에서는 변경 예정
        Set<String> keys = sredis.keys("views:content:*:count");
        if (keys == null || keys.isEmpty()) return;

        for (String k : keys) {
            String val = sredis.opsForValue().get(k);
            if (val == null) continue;

            // 삭제
            sredis.delete(k);

            long delta = Long.parseLong(val);
            Long id = Long.parseLong(k.split(":")[2]);

            contentRepository.findById(id).ifPresent(e -> {
                long cur = e.getLocalViewCount() == null ? 0 : e.getLocalViewCount();
                e.setLocalViewCount(cur + delta);
                contentRepository.save(e);
                esContentRepository.save(toEsContent(e));

                log.debug("Flushed view count to DB and ES for id={}", id);
            });
        }
    }

    /**
     * 특정 날짜에 생성된 콘텐츠 수를 반환 (내부 통계용).
     */
    public long countByDay(LocalDate date) {
        return contentRepository.countByCreatedAt(date);
    }

    /**
     * 카테고리별 콘텐츠 개수 조회 (source별 필터링 기능 추가)
     */
    public Map<String, Long> getCategoryCounts(String source) {
        List<CategoryCountDto> counts;
        long totalCount;

        // source 파라미터 유무에 따라 분기
        if (source != null && !source.isBlank()) {
            // source가 있을 경우: 해당 source로 필터링
            counts = contentRepository.countByCategoryAndSource(source.toUpperCase());
            totalCount = contentRepository.countBySource(source.toUpperCase());
        } else {
            // source가 없을 경우: 전체 집계
            counts = contentRepository.countByCategory();
            totalCount = contentRepository.count();
        }

        // 결과를 Map으로 변환
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        categoryCounts.put("전체", totalCount);
        for (CategoryCountDto dto : counts) {
            categoryCounts.put(dto.getCategory(), dto.getCount());
        }

        return categoryCounts;
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
                .status(e.getStatus() != null ? e.getStatus().name() : "ACTIVE")
                .build();
    }
}
