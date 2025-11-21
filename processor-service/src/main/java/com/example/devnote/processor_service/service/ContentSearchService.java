package com.example.devnote.processor_service.service;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentSearchService {
    private final EsSearchLogRepository searchLogRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;

    private static final Pattern SAFE_SEARCH_TERM =
            Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s._\\-+#]{1,50}$");

    /**
     * 키워드로 콘텐츠를 검색하고, IP기반으로 10분에 한 번씩 검색어 로그 남기기
     */
    public Page<EsContent> search(String keyword, String source, String category, String sortOption, Pageable pageable, HttpServletRequest request) {
        // 검색어 유효성 검사
        if (!isValidSearchTerm(keyword)) {
            return Page.empty(pageable);
        }

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

    // 유효성 검사 로직
    private boolean isValidSearchTerm(String term) {
        if (term == null) return false;

        String trimmed = term.trim();
        if (trimmed.isEmpty()) return false;

        // 정규식 매칭 여부 확인
        return SAFE_SEARCH_TERM.matcher(trimmed).matches();
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

    /**
     * 'search_as_you_type' 필드를 사용하여 검색어 제안 목록을 반환
     * @param keyword 사용자가 입력 중인 키워드
     * @param source 필터링할 소스
     * @return 추천 검색어 목록 (콘텐츠 제목 또는 채널명)
     */
    public List<String> getSuggestions(String keyword, String source) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        // AND 조건으로 묶기 위해 bool 쿼리 사용
                        .bool(b -> b
                                // 1. 키워드 매칭 조건
                                .must(m -> m
                                        .multiMatch(mm -> mm
                                                .query(keyword)
                                                .type(TextQueryType.BoolPrefix)
                                                .fields("title.suggest", "channelTitle.suggest")
                                        )
                                )
                                // 2. 소스(YOUTUBE/NEWS) 필터링 조건
                                .filter(f -> f
                                        .term(t -> t
                                                .field("source")
                                                .value(source.toUpperCase())
                                        )
                                )
                        )
                )
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<EsContent> searchHits = elasticsearchOperations.search(query, EsContent.class);

        // 검색된 문서의 원본 제목과 채널명을 중복 없이 리스트로 만들어 반환
        return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getTitle())
                .distinct()
                .collect(Collectors.toList());
    }
}
