package com.example.devnote.stats_service.service;

import com.example.devnote.stats_service.dto.DailyCountDto;
import com.example.devnote.stats_service.dto.MonthlyCountDto;
import com.example.devnote.stats_service.dto.YearlyCountDto;
import com.example.devnote.stats_service.entity.PageDurationDailyStats;
import com.example.devnote.stats_service.repository.PageDurationDailyStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 페이지 체류 시간 통계 서비스
 * - 프론트엔드의 하트비트 신호를 받아 Redis에 실시간으로 세션 정보를 기록
 * - 1분 주기의 스케줄러로 종료된 세션을 정리하며 일일 총 체류 시간을 Redis에 누적
 * - 자정 스케줄러로 Redis의 최종 집계치를 DB에 영속화
 * - 일/월/연 단위 조회 API 제공
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DurationStatsService {

    private final StringRedisTemplate redis;
    private final PageDurationDailyStatsRepository repo;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int HEARTBEAT_INTERVAL_SECONDS = 15;
    private static final int SESSION_TIMEOUT_SECONDS = HEARTBEAT_INTERVAL_SECONDS * 3; // 45초

    // Redis 키 포맷
    private String sessionKey(String pageViewId) { return "duration:view:" + pageViewId; }
    private String activeSessionKey() { return "duration:active_sessions"; }
    private String dailyTotalKey(LocalDate day) { return "stats:duration:total:day:" + DAY_FMT.format(day); }

    /**
     * 프론트엔드로부터 하트비트 요청을 받아 Redis에 기록
     */
    public void handleHeartbeat(String pageViewId) {
        long now = System.currentTimeMillis();
        String sessionKey = sessionKey(pageViewId);

        // 키가 없을 때만 startTime을 기록 (첫 하트비트)
        redis.opsForHash().putIfAbsent(sessionKey, "startTime", String.valueOf(now));

        // 마지막 확인 시간은 항상 갱신
        redis.opsForHash().put(sessionKey, "lastSeen", String.valueOf(now));

        // 키는 넉넉하게 10분 뒤 만료되도록 설정
        redis.expire(sessionKey, Duration.ofMinutes(10));

        // 활성 세션 목록(Sorted Set)에 마지막 확인 시간을 점수(score)로 하여 추가
        // 시간 초과된 세션을 효율적으로 찾기 위함
        redis.opsForZSet().add(activeSessionKey(), pageViewId, now);
    }

    /**
     * 1분마다 실행되어 응답이 없는 세션(타임아웃)을 종료 처리하고 체류 시간 집계
     */
    @Scheduled(fixedDelayString = "60000")
    public void reapEndedSessions() {
        long timeoutThreshold = System.currentTimeMillis() - (SESSION_TIMEOUT_SECONDS * 1000L);

        // 1. 타임아웃된 세션 ID들을 Sorted Set에서 찾기
        Set<String> expiredSessionIds = redis.opsForZSet().rangeByScore(activeSessionKey(), 0, timeoutThreshold);
        if (expiredSessionIds == null || expiredSessionIds.isEmpty()) {
            return;
        }
        log.info("[STATS-DURATION] Found {} expired sessions to reap.", expiredSessionIds.size());

        for (String pageViewId : expiredSessionIds) {
            String sessionKey = sessionKey(pageViewId);
            // 2. 세션 정보(시작시간, 마지막 확인시간)를 Redis Hash에서 가져오기
            Map<Object, Object> sessionData = redis.opsForHash().entries(sessionKey);
            String startStr = (String) sessionData.get("startTime");
            String lastSeenStr = (String) sessionData.get("lastSeen");

            if (startStr != null && lastSeenStr != null) {
                long startTime = Long.parseLong(startStr);
                long lastSeenTime = Long.parseLong(lastSeenStr);
                long durationSeconds = (lastSeenTime - startTime) / 1000; // 초 단위로 변환

                // 3. 오늘 날짜의 총 체류 시간에 합산
                LocalDate today = LocalDate.now(ZONE);
                redis.opsForValue().increment(dailyTotalKey(today), durationSeconds);
                redis.expire(dailyTotalKey(today), Duration.ofDays(2)); // TTL 설정
            }

            // 4. 처리 완료된 세션 정보(Hash) 및 활성 목록(ZSet)에서 삭제
            redis.delete(sessionKey);
            redis.opsForZSet().remove(activeSessionKey(), pageViewId);
        }
    }

    /**
     * 매일 자정에 어제 날짜의 Redis 카운트를 DB에 저장 (스케줄링)
     */
    @Scheduled(cron = "0 3 0 * * *", zone = "Asia/Seoul") // 새벽 00:03에 실행
    @Transactional
    public void flushYesterdayStatsToDb() {
        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
        String key = dailyTotalKey(yesterday);
        String value = redis.opsForValue().get(key);
        long totalSeconds = (value != null) ? Long.parseLong(value) : 0L;

        log.info("[STATS-DURATION] Flushing yesterday's ({}) total duration ({}s) to DB.", yesterday, totalSeconds);

        PageDurationDailyStats stats = repo.findByDay(yesterday)
                .orElse(new PageDurationDailyStats(null, yesterday, 0L));
        stats.setTotalDurationSeconds(totalSeconds);
        repo.save(stats);

        redis.delete(key);
    }

    /**
     * 오늘 총 체류시간 (실시간)
     */
    private long getTodayTotalSeconds() {
        String value = redis.opsForValue().get(dailyTotalKey(LocalDate.now(ZONE)));
        return (value != null) ? Long.parseLong(value) : 0L;
    }

    /**
     * 기간별 일별 총 체류시간 조회
     */
    public List<DailyCountDto> getDailyStats(LocalDate start, LocalDate end) {
        Map<LocalDate, Long> dbDurations = repo.findDailyDurationsByRange(start, end).stream()
                .collect(Collectors.toMap(row -> (LocalDate) row[0], row -> (long) row[1]));
        List<DailyCountDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now(ZONE);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            long duration = date.equals(today) ? getTodayTotalSeconds() : dbDurations.getOrDefault(date, 0L);
            result.add(new DailyCountDto(date.format(DateTimeFormatter.ISO_LOCAL_DATE), duration));
        }
        return result;
    }

    /**
     * 특정 연도의 월별 총 체류시간 조회
     */
    public List<MonthlyCountDto> getMonthlyStats(int year) {
        Map<Integer, Long> dbDurations = repo.findMonthlyDurationsByYear(year).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> (long) row[1]));

        LocalDate today = LocalDate.now(ZONE);
        if (today.getYear() == year) {
            dbDurations.merge(today.getMonthValue(), getTodayTotalSeconds(), Long::sum);
        }

        List<MonthlyCountDto> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            result.add(new MonthlyCountDto(month, dbDurations.getOrDefault(month, 0L)));
        }
        return result;
    }

    /**
     * 기간별 연도별 총 체류시간 조회
     */
    public List<YearlyCountDto> getYearlyStats(int startYear, int endYear) {
        Map<Integer, Long> dbDurations = repo.findYearlyDurationsByRange(startYear, endYear).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> (long) row[1]));

        LocalDate today = LocalDate.now(ZONE);
        if (today.getYear() >= startYear && today.getYear() <= endYear) {
            dbDurations.merge(today.getYear(), getTodayTotalSeconds(), Long::sum);
        }

        List<YearlyCountDto> result = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            result.add(new YearlyCountDto(year, dbDurations.getOrDefault(year, 0L)));
        }
        return result;
    }
}