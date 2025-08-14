package com.example.devnote.stats_service.service;

import com.example.devnote.stats_service.dto.DailyCountDto;
import com.example.devnote.stats_service.dto.MonthlyCountDto;
import com.example.devnote.stats_service.dto.YearlyCountDto;
import com.example.devnote.stats_service.entity.CommentDailyStats;
import com.example.devnote.stats_service.repository.CommentDailyStatsRepository;
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
 * 신규 댓글 생성 통계 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentStatsService {

    private final StringRedisTemplate redis;
    private final CommentDailyStatsRepository repo;
    private final WebClient.Builder webClientBuilder;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String TOPIC_COMMENT_CREATED = "comment.created";

    // Redis 키 포맷
    private String dayCountKey(LocalDate day) {
        return "stats:comment:new:day:" + day.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * Kafka 'comment.created' 토픽 리스너
     * - 메시지 수신 시 오늘 날짜의 신규 댓글 카운터를 Redis에서 1 증가시킴
     */
    @KafkaListener(topics = TOPIC_COMMENT_CREATED, groupId = "stats-service-group-comment")
    public void onCommentCreated(String message) {
        LocalDate today = LocalDate.now(ZONE);
        String key = dayCountKey(today);
        log.debug("Incrementing new comment count for key: {}", key);
        redis.opsForValue().increment(key);
        redis.expire(key, Duration.ofDays(2));
    }

    /**
     * 매일 자정에 어제 날짜의 Redis 카운트를 DB에 저장 (스케줄링)
     */
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul") // 콘텐츠 통계와 1분 차이
    @Transactional
    public void flushYesterdayStatsToDb() {
        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
        flushStatsToDbForDate(yesterday);
    }

    /**
     * 특정 날짜의 Redis 카운트를 DB에 저장/업데이트하는 메서드
     * @param dateToFlush 통계를 DB에 저장할 날짜
     */
    @Transactional
    public void flushStatsToDbForDate(LocalDate dateToFlush) {
        long count = 0;
        try {
            WebClient webClient = webClientBuilder.baseUrl("http://user-service").build();
            Map<String, Long> response = webClient.get()
                    .uri("/internal/stats/comment/count-by-day?date={date}", dateToFlush)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {})
                    .block();

            if (response != null && response.containsKey("count")) {
                count = response.get("count");
            }
        } catch (Exception e) {
            log.error("[STATS-COMMENT] Failed to fetch count from user-service for date {}: {}", dateToFlush, e.getMessage());
            count = 0;
        }

        log.info("[STATS-COMMENT] Flushing date '{}' new comment count ({}) to DB.", dateToFlush, count);

        CommentDailyStats stats = repo.findByDay(dateToFlush)
                .orElse(new CommentDailyStats(null, dateToFlush, 0L));
        stats.setCount(count);
        repo.save(stats);
    }

    /**
     * 지정된 기간 동안의 통계를 Redis에서 읽어와 DB에 저장
     * @param start 시작일
     * @param end 종료일
     */
    public void backfillHistoricalStats(LocalDate start, LocalDate end) {
        log.info("[STATS-COMMENT][BACKFILL] Starting backfill from {} to {}", start, end);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            flushStatsToDbForDate(date);
        }
        log.info("[STATS-COMMENT][BACKFILL] Completed backfill.");
    }

    /**
     * 오늘 신규 댓글 수 (실시간)
     */
    private long getTodayCount() {
        LocalDate today = LocalDate.now(ZONE);
        String value = redis.opsForValue().get(dayCountKey(today));
        return (value != null) ? Long.parseLong(value) : 0L;
    }

    /**
     * 기간별 일별 신규 댓글 수 조회
     */
    public List<DailyCountDto> getDailyStats(LocalDate start, LocalDate end) {
        Map<LocalDate, Long> dbCounts = repo.findDailyCountsByRange(start, end).stream()
                .collect(Collectors.toMap(row -> (LocalDate) row[0], row -> (long) row[1]));
        List<DailyCountDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now(ZONE);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            long count = date.equals(today) ? getTodayCount() : dbCounts.getOrDefault(date, 0L);
            result.add(new DailyCountDto(date.format(DateTimeFormatter.ISO_LOCAL_DATE), count));
        }
        return result;
    }

    /**
     * 특정 연도의 월별 신규 댓글 수 조회
     */
    public List<MonthlyCountDto> getMonthlyStats(int year) {
        Map<Integer, Long> dbCounts = repo.findMonthlyCountsByYear(year).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> (long) row[1]));
        LocalDate today = LocalDate.now(ZONE);
        if (today.getYear() == year) {
            dbCounts.merge(today.getMonthValue(), getTodayCount(), Long::sum);
        }
        List<MonthlyCountDto> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            result.add(new MonthlyCountDto(month, dbCounts.getOrDefault(month, 0L)));
        }
        return result;
    }

    /**
     * 기간별 연도별 신규 댓글 수 조회
     */
    public List<YearlyCountDto> getYearlyStats(int startYear, int endYear) {
        Map<Integer, Long> dbCounts = repo.findYearlyCountsByRange(startYear, endYear).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> (long) row[1]));
        LocalDate today = LocalDate.now(ZONE);
        if (today.getYear() >= startYear && today.getYear() <= endYear) {
            dbCounts.merge(today.getYear(), getTodayCount(), Long::sum);
        }
        List<YearlyCountDto> result = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            result.add(new YearlyCountDto(year, dbCounts.getOrDefault(year, 0L)));
        }
        return result;
    }
}