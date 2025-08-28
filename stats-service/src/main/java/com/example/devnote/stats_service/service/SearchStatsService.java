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

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchStatsService {

    private final EsSearchLogRepository searchLogRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String RANKING_KEY = "ranking:search_terms";

    /**
     * 10분마다 실행되어 인기 검색어 랭킹을 갱신
     */
    @Scheduled(fixedRate = 600000, initialDelay = 60000)
    public void updateUserSearchRanking() {
        log.info("[SCHEDULED] Starting popular search term ranking update.");

        Instant now = Instant.now();
        Map<String, Double> scoreMap = new HashMap<>();

        // 1. 시간대별로 가중치를 다르게 하여 검색 로그를 조회하고 점수를 합산
        // - 최근 1시간 내 검색어: 10점
        addScores(scoreMap, searchLogsBetween(now.minus(1, ChronoUnit.HOURS), now), 10.0);
        // - 1시간 전 ~ 6시간 전 검색어: 5점
        addScores(scoreMap, searchLogsBetween(now.minus(6, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS)), 5.0);
        // - 6시간 전 ~ 24시간 전 검색어: 1점
        addScores(scoreMap, searchLogsBetween(now.minus(24, ChronoUnit.HOURS), now.minus(6, ChronoUnit.HOURS)), 1.0);

        // 2. Redis에 새로운 랭킹을 업데이트
        if (!scoreMap.isEmpty()) {
            String tempKey = RANKING_KEY + ":temp";
            redisTemplate.delete(tempKey); // 임시 키 초기화

            scoreMap.forEach((query, score) ->
                    redisTemplate.opsForZSet().add(tempKey, query, score)
            );

            // 임시 키의 이름을 실제 랭킹 키로 변경
            redisTemplate.rename(tempKey, RANKING_KEY);
            log.info("Updated popular search term ranking with {} terms.", scoreMap.size());
        } else {
            redisTemplate.delete(RANKING_KEY); // 집계된 결과가 없으면 기존 랭킹 삭제
            log.info("No recent search terms to rank. Cleared existing ranking.");
        }
    }

    /**
     * Elasticsearch에서 특정 기간의 검색 로그 조회 메서드
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
            scoreMap.merge(log.getQuery(), weight, Double::sum);
        }
    }
}