//package com.example.devnote.stats_service.service;
//
//import com.example.devnote.stats_service.dto.DailyCountDto;
//import com.example.devnote.stats_service.dto.MonthlyCountDto;
//import com.example.devnote.stats_service.dto.YearlyCountDto;
//import com.example.devnote.stats_service.entity.ContentViewDailyStats;
//import com.example.devnote.stats_service.repository.ContentViewDailyStatsRepository;
//import com.example.devnote.stats_service.repository.TrafficMinuteRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * 콘텐츠 조회수 통계 서비스
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ContentViewStatsService {
//
//    private final StringRedisTemplate redis;
//    private final ContentViewDailyStatsRepository viewRepo;
//    private final TrafficMinuteRepository trafficRepo;
//
//    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
//    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
//
//    // Redis 키 포맷
//    private String dayCountKey(LocalDate day) {
//        return "stats:content_view:day:" + DAY_FMT.format(day);
//    }
//
//    /**
//     * 매일 자정에 어제 날짜의 Redis 카운트를 DB에 저장 (스케줄링)
//     */
//    @Scheduled(cron = "0 2 0 * * *", zone = "Asia/Seoul") // 다른 스케줄러와 겹치지 않게 2분에 실행
//    @Transactional
//    public void flushYesterdayStatsToDb() {
//        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
//        String key = dayCountKey(yesterday);
//        String value = redis.opsForValue().get(key);
//        long count = (value != null) ? Long.parseLong(value) : 0L;
//
//        log.info("[STATS-VIEWS] Flushing yesterday's ({}) view count ({}) from Redis to DB.", yesterday, count);
//
//        ContentViewDailyStats stats = viewRepo.findByDay(yesterday)
//                .orElse(new ContentViewDailyStats(null, yesterday, 0L));
//        stats.setCount(count);
//        viewRepo.save(stats);
//
//        redis.delete(key);
//    }
//
//    /**
//     * 특정 날짜의 통계를 DB에 저장/업데이트 (백필용)
//     */
//    @Transactional
//    public void flushStatsToDbForDate(LocalDate dateToFlush) {
//        // 백필은 traffic_minute 테이블(Source of Truth)에서 직접 집계
//        long count = trafficRepo.sumContentViewsByDay(dateToFlush);
//
//        log.info("[STATS-VIEWS] Flushing date '{}' view count ({}) from DB to DB.", dateToFlush, count);
//
//        ContentViewDailyStats stats = viewRepo.findByDay(dateToFlush)
//                .orElse(new ContentViewDailyStats(null, dateToFlush, 0L));
//        stats.setCount(count);
//        viewRepo.save(stats);
//    }
//
//    /**
//     * 지정된 기간 동안의 통계를 DB 원본 데이터 기준으로 다시 계산하여 저장
//     */
//    public void backfillHistoricalStats(LocalDate start, LocalDate end) {
//        log.info("[STATS-VIEWS][BACKFILL] Starting backfill from {} to {}", start, end);
//        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
//            flushStatsToDbForDate(date);
//        }
//        log.info("[STATS-VIEWS][BACKFILL] Completed backfill.");
//    }
//
//    /**
//     * 오늘 콘텐츠 조회수 수 (실시간)
//     */
//    private long getTodayCount() {
//        LocalDate today = LocalDate.now(ZONE);
//        String value = redis.opsForValue().get(dayCountKey(today));
//        return (value != null) ? Long.parseLong(value) : 0L;
//    }
//
//    /**
//     * 기간별 일별 콘텐츠 조회수조회
//     */
//    public List<DailyCountDto> getDailyStats(LocalDate start, LocalDate end) {
//        Map<LocalDate, Long> dbCounts = viewRepo.findDailyCountsByRange(start, end).stream()
//                .collect(Collectors.toMap(row -> (LocalDate) row[0], row -> (long) row[1]));
//        List<DailyCountDto> result = new ArrayList<>();
//        LocalDate today = LocalDate.now(ZONE);
//        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
//            long count = date.equals(today) ? getTodayCount() : dbCounts.getOrDefault(date, 0L);
//            result.add(new DailyCountDto(date.format(DateTimeFormatter.ISO_LOCAL_DATE), count));
//        }
//        return result;
//    }
//
//    /**
//     * 특정 연도의 월별 콘텐츠 조회수조회
//     */
//    public List<MonthlyCountDto> getMonthlyStats(int year) {
//        Map<Integer, Long> dbCounts = viewRepo.findMonthlyCountsByYear(year).stream()
//                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> (long) row[1]));
//        LocalDate today = LocalDate.now(ZONE);
//        if (today.getYear() == year) {
//            dbCounts.merge(today.getMonthValue(), getTodayCount(), Long::sum);
//        }
//        List<MonthlyCountDto> result = new ArrayList<>();
//        for (int month = 1; month <= 12; month++) {
//            result.add(new MonthlyCountDto(month, dbCounts.getOrDefault(month, 0L)));
//        }
//        return result;
//    }
//
//    /**
//     * 기간별 연도별 콘텐츠 조회수 조회
//     */
//    public List<YearlyCountDto> getYearlyStats(int startYear, int endYear) {
//        Map<Integer, Long> dbCounts = viewRepo.findYearlyCountsByRange(startYear, endYear).stream()
//                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> (long) row[1]));
//        LocalDate today = LocalDate.now(ZONE);
//        if (today.getYear() >= startYear && today.getYear() <= endYear) {
//            dbCounts.merge(today.getYear(), getTodayCount(), Long::sum);
//        }
//        List<YearlyCountDto> result = new ArrayList<>();
//        for (int year = startYear; year <= endYear; year++) {
//            result.add(new YearlyCountDto(year, dbCounts.getOrDefault(year, 0L)));
//        }
//        return result;
//    }
//}
