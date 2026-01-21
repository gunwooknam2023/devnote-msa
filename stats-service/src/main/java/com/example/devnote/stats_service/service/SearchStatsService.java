package com.example.devnote.stats_service.service;

import com.example.devnote.stats_service.es.EsSearchLog;
import com.example.devnote.stats_service.es.EsSearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchStatsService {

    private final EsSearchLogRepository searchLogRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String RANKING_KEY_PREFIX = "ranking:search_terms:";

    private static final Pattern SAFE_SEARCH_TERM =
            Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s._\\-+#]{1,50}$");

    /**
     * 10분마다 실행, 서버 기동 즉시 모든 소스(YOUTUBE, NEWS) 랭킹 업데이트
     */
    @Scheduled(fixedRate = 600000, initialDelay = 0)
    public void updateAllRankings() {
        log.info("[SCHEDULED] Starting popular search term ranking update for YOUTUBE and NEWS (1 Month Range)");

        // 1달간의 데이터를 각 소스별로 나누어 집계 실행
        updateRankingBySource("YOUTUBE");
        updateRankingBySource("NEWS");
    }

    /**
     * 특정 소스에 대해 최근 1달간의 데이터에 가중치를 적용하여 Redis에 저장
     */
    private void updateRankingBySource(String source) {
        Instant now = Instant.now();
        Map<String, Double> scoreMap = new HashMap<>();

        // 1. 최근 1일 내 검색어: 10점
        addScores(scoreMap, getLogs(source, now.minus(1, ChronoUnit.DAYS), now), 10.0);
        // 2. 1일 전 ~ 7일 전 검색어: 5점
        addScores(scoreMap, getLogs(source, now.minus(7, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS)), 5.0);
        // 3. 7일 전 ~ 30일 전 검색어: 1점
        addScores(scoreMap, getLogs(source, now.minus(30, ChronoUnit.DAYS), now.minus(7, ChronoUnit.DAYS)), 1.0);

        String rankingKey = RANKING_KEY_PREFIX + source;

        if (!scoreMap.isEmpty()) {
            String tempKey = rankingKey + ":temp";
            redisTemplate.delete(tempKey);

            scoreMap.forEach((query, score) ->
                    redisTemplate.opsForZSet().add(tempKey, query, score)
            );

            redisTemplate.rename(tempKey, rankingKey);
            log.info("Updated [{}] ranking with {} terms.", source, scoreMap.size());
        } else {
            redisTemplate.delete(rankingKey);
            log.info("No recent terms found for [{}]. Cleared ranking.", source);
        }
    }

    /**
     * Elasticsearch에서 특정 기간의 검색 로그 조회 메서드
     */
    private List<EsSearchLog> getLogs(String source, Instant start, Instant end) {
        PageRequest pageable = PageRequest.of(0, 10000);
        return searchLogRepository.findBySourceAndTimestampBetween(source, start, end, pageable);
    }

    /**
     * (통합집계, 현재 사용 x -> 2026.01.21 ~ ) Elasticsearch에서 특정 기간의 검색 로그 조회 메서드
     */
    private List<EsSearchLog> searchLogsBetween(Instant start, Instant end) {
        PageRequest pageable = PageRequest.of(0, 10000);
        return searchLogRepository.findByTimestampBetween(start, end, pageable);
    }


    /**
     * 조회된 로그 목록을 바탕으로 점수 맵에 가중치를 더하는 메서드
     */
    private void addScores(Map<String, Double> scoreMap, List<EsSearchLog> logs, double weight) {
        for (EsSearchLog log : logs) {
            String query = log.getQuery();
            if (isValidSearchTerm(query)) {
                scoreMap.merge(query, weight, Double::sum);
            }
        }
    }

    // 유효성 검사 로직
    private boolean isValidSearchTerm(String term) {
        if (term == null) return false;

        String trimmed = term.trim();
        if (trimmed.isEmpty()) return false;

        // 정규식 매칭 여부 확인
        return SAFE_SEARCH_TERM.matcher(trimmed).matches();
    }
}