package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.es.EsContentRepository;
import com.example.devnote.processor_service.es.EsSearchLog;
import com.example.devnote.processor_service.es.EsSearchLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
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
    public Page<EsContent> search(String keyword, Pageable pageable, HttpServletRequest request) {
        // 검색어 로깅 시도
        logSearchQuery(keyword, request);

        // 'title', 'description', 'channelTitle' 필드에서 키워드를 검색하는 쿼리를 생성
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(mm -> mm
                                .query(keyword)
                                .fields("title", "description", "channelTitle")
                        )
                )
                .withPageable(pageable)
                .build();

        // 쿼리 실행
        SearchHits<EsContent> searchHits = elasticsearchOperations.search(query, EsContent.class);

        // SearchHits를 Page 객체로 변환 후 반환
        return org.springframework.data.support.PageableExecutionUtils.getPage(
                searchHits.getSearchHits().stream().map(org.springframework.data.elasticsearch.core.SearchHit::getContent).toList(),
                pageable,
                searchHits::getTotalHits
        );
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
