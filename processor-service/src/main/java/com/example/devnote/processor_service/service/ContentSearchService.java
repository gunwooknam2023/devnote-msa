package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.es.EsSearchLog;
import com.example.devnote.processor_service.es.EsSearchLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContentSearchService {
    private final EsSearchLogRepository searchLogRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;

    /**
     * 키워드로 콘텐츠를 검색하고, IP기반으로 10분에 한 번씩 검색어 로그 남기기
     */
    public Page<EsContent> search(String keyword, String source, String category, String sortOption, Pageable pageable, HttpServletRequest request) {
        // 검색어 로깅 시도
        logSearchQuery(keyword, request);

        Sort sort = buildSort(sortOption);
        Pageable finalPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // Bool 쿼리 구조를 must와 filter로 분리
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            // 1. 점수 계산에 영향을 주는 검색어 쿼리는 'must' 절에 삽입
                            b.must(m -> m
                                    .multiMatch(mm -> mm
                                            .query(keyword)
                                            .fields("title", "description", "channelTitle")
                                    )
                            );

                            // 2. 점수 계산 없이 필터링만 하는 조건들은 'filter' 절에 삽입
                            b.filter(f -> f
                                    .term(t -> t
                                            .field("source")
                                            .value(source.toUpperCase())
                                    )
                            );

                            // 3. category 파라미터가 있을 경우에만 category 필터 조건을 추가
                            if (category != null && !category.isBlank()) {
                                b.filter(f -> f
                                        .term(t -> t
                                                .field("category")
                                                .value(category)
                                        )
                                );
                            }

                            return b;
                        })
                )
                .withPageable(finalPageable)
                .build();

        SearchHits<EsContent> searchHits = elasticsearchOperations.search(query, EsContent.class);

        return org.springframework.data.support.PageableExecutionUtils.getPage(
                searchHits.getSearchHits().stream().map(org.springframework.data.elasticsearch.core.SearchHit::getContent).toList(),
                finalPageable,
                searchHits::getTotalHits
        );
    }


    /**
     * 파라미터 값에 따라 Sort 객체를 생성하는 메서드
     */
    private Sort buildSort(String sortOption) {
        return switch (sortOption.toLowerCase()) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "publishedAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");

            // relevance(정확도순)일 경우, 정렬 없음을 명시하면 Elasticsearch 기본값인 연관도 점수(_score)로 정렬
            default -> Sort.unsorted();
        };
    }

    /**
     * 검색어 로깅 (IP 기반 10분 제한 추가)
     */
    private void logSearchQuery(String query, HttpServletRequest request) {
        // 1. 요청자 IP 추출
        String ipAddress = Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElse(request.getRemoteAddr());
        String trimmedQuery = query.trim();

        // 2. Redis에 사용할 키 생성
        String redisKey = "search_log_limit:ip:" + ipAddress + ":" + trimmedQuery;

        // 3. Redis에 해당 키가 있는지 확인. setIfAbsent는 키가 없을 때만 true를 반환
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofMinutes(10));

        // 4. 키가 성공적으로 세팅되었다면 (10분 내에 처음 검색한 키워드라면) 로그 저장
        if (Boolean.TRUE.equals(isFirstTime)) {
            EsSearchLog log = EsSearchLog.builder()
                    .query(trimmedQuery)
                    .timestamp(Instant.now())
                    .build();
            searchLogRepository.save(log);
        }
    }
}
