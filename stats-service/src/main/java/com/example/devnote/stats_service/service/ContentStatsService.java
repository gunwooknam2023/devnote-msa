package com.example.devnote.stats_service.service;


import com.example.devnote.stats_service.dto.DailyCountDto;
import com.example.devnote.stats_service.dto.MonthlyCountDto;
import com.example.devnote.stats_service.dto.YearlyCountDto;
import com.example.devnote.stats_service.entity.ContentDailyStats;
import com.example.devnote.stats_service.repository.ContentDailyStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 신규 콘텐츠 생성 통계 서비스
 * - Kafka로 생성 이벤트를 받아 Redis에 실시간 카운트
 * - 매일 자정 스케줄러로 Redis 카운트를 DB에 영속화
 * - 일/월/연 단위 조회 API 제공
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentStatsService {

    private final StringRedisTemplate redis;
    private final ContentDailyStatsRepository repo;
    private final WebClient.Builder webClientBuilder;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String TOPIC_CONTENT_CREATED = "content.created";

    // Redis 키 포맷
    private String dayCountKey(LocalDate day) {
        return "stats:content:new:day:" + day.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * Kafka 'content.created' 토픽 리스너
     * - 메시지 수신 시 오늘 날짜의 신규 콘텐츠 카운터를 Redis에서 1 증가시킴
     */
    @KafkaListener(topics = TOPIC_CONTENT_CREATED, groupId = "stats-service-group-content")
    public void onContentCreated(String message) {
        // 메시지 내용은 현재 사용하지 않지만, 추후 확장을 위해 받을 수 있음
        LocalDate today = LocalDate.now(ZONE);
        String key = dayCountKey(today);
        log.debug("Incrementing new content count for key: {}", key);
        redis.opsForValue().increment(key);
        // 키가 새로 생성된 경우 TTL 설정 (2일)
        redis.expire(key, Duration.ofDays(2));
    }

    /**
     * 매일 자정에 어제 날짜의 Redis 카운트를 DB에 저장 (스케줄링)
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void flushYesterdayStatsToDb() {
        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
        flushStatsToDbForDate(yesterday);
    }

    /**
     * 특정 날짜의 Redis 카운트를 DB에 저장/업데이트하는 메서드
     */
    @Transactional
    public void flushStatsToDbForDate(LocalDate dateToFlush) {
        long count = 0;
        try {
            WebClient webClient = webClientBuilder.baseUrl("http://processor-service").build();
            Map<String, Long> response = webClient.get()
                    .uri("/internal/stats/content/count-by-day?date={date}", dateToFlush)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {})
                    .block(); // 동기 방식으로 결과를 기다림

            if (response != null && response.containsKey("count")) {
                count = response.get("count");
            }
        } catch (Exception e) {
            log.error("[STATS-CONTENT] Failed to fetch count from processor-service for date {}: {}", dateToFlush, e.getMessage());
            // 에러 발생 시 0으로 처리
            count = 0;
        }

        log.info("[STATS-CONTENT] Flushing date '{}' new content count ({}) to DB.", dateToFlush, count);

        ContentDailyStats stats = repo.findByDay(dateToFlush)
                .orElse(new ContentDailyStats(null, dateToFlush, 0L));
        stats.setCount(count);
        repo.save(stats);
    }

    /**
     * 지정된 기간 동안의 통계를 Redis에서 읽어와 DB에 저장
     * @param start 시작일
     * @param end 종료일
     */
    public void backfillHistoricalStats(LocalDate start, LocalDate end) {
        log.info("[STATS-CONTENT][BACKFILL] Starting backfill from {} to {}", start, end);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            flushStatsToDbForDate(date);
        }
        log.info("[STATS-CONTENT][BACKFILL] Completed backfill.");
    }

    /**
     * 오늘 신규 콘텐츠 수 (실시간)
     */
    private long getTodayCount() {
        LocalDate today = LocalDate.now(ZONE);
        String value = redis.opsForValue().get(dayCountKey(today));
        return (value != null) ? Long.parseLong(value) : 0L;
    }

    /**
     * 기간별 일별 신규 콘텐츠 수 조회
     */
    public List<DailyCountDto> getDailyStats(LocalDate start, LocalDate end) {
        Map<LocalDate, Long> dbCounts = repo.findDailyCountsByRange(start, end).stream()
                .collect(Collectors.toMap(
                        row -> (LocalDate) row[0],
                        row -> (long) row[1]
                ));

        List<DailyCountDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now(ZONE);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            long count = date.equals(today)
                    ? getTodayCount()
                    : dbCounts.getOrDefault(date, 0L);

            result.add(new DailyCountDto(date.format(DateTimeFormatter.ISO_LOCAL_DATE), count));
        }
        return result;
    }

    /**
     * 특정 연도의 월별 신규 콘텐츠 수 조회
     */
    public List<MonthlyCountDto> getMonthlyStats(int year) {
        Map<Integer, Long> dbCounts = repo.findMonthlyCountsByYear(year).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> (long) row[1]
                ));

        // 오늘의 데이터는 아직 DB에 없으므로, 현재 연도/월 데이터에 Redis 값을 합산
        LocalDate today = LocalDate.now(ZONE);
        if (today.getYear() == year) {
            long todayCount = getTodayCount();
            dbCounts.merge(today.getMonthValue(), todayCount, Long::sum);
        }

        List<MonthlyCountDto> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            result.add(new MonthlyCountDto(month, dbCounts.getOrDefault(month, 0L)));
        }
        return result;
    }

    /**
     * 기간별 연도별 신규 콘텐츠 수 조회
     */
    public List<YearlyCountDto> getYearlyStats(int startYear, int endYear) {
        Map<Integer, Long> dbCounts = repo.findYearlyCountsByRange(startYear, endYear).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> (long) row[1]
                ));

        // 오늘의 데이터는 아직 DB에 없으므로, 현재 연도 데이터에 합산
        LocalDate today = LocalDate.now(ZONE);
        if (today.getYear() >= startYear && today.getYear() <= endYear) {
            long todayCount = getTodayCount();
            dbCounts.merge(today.getYear(), todayCount, Long::sum);
        }

        List<YearlyCountDto> result = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            result.add(new YearlyCountDto(year, dbCounts.getOrDefault(year, 0L)));
        }
        return result;
    }
}