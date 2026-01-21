package com.example.devnote.stats_service.service;

import com.example.devnote.stats_service.dto.*;
import com.example.devnote.stats_service.entity.VisitEvent;
import com.example.devnote.stats_service.repository.VisitEventRepository;
import com.example.devnote.stats_service.util.Hashing;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 방문자 통계 서비스
 * - 24시간(하루) 단위 중복 방지 → 같은 방문자 하루 최대 1회 카운트
 * - Redis에 일/시간 카운터를 올려 실시간 조회 가속화
 * - DB에는 VisitEvent(버킷 단위 1행)로 영속
 */
@Service
@RequiredArgsConstructor
public class VisitService {
    private final VisitEventRepository repo;
    private final StringRedisTemplate redis;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    // Redis 키 포맷
    private String dayKey(LocalDate day) { return "visit:count:day:" + DAY_FMT.format(day); }
    private String hourKey(LocalDate day, int hour) { return "visit:count:hour:" + DAY_FMT.format(day) + ":" + String.format("%02d", hour); }
    private String dedupKey(String visitorHash, Instant bucketStart) { return "visit:dedup:" + visitorHash + ":" + bucketStart.toEpochMilli(); }

    /** 현재 날짜의 시작 시각(00:00:00) */
    private ZonedDateTime bucketStart(ZonedDateTime now) {
        return now.toLocalDate().atStartOfDay(ZONE);
    }

    /**
     * 방문자 식별 해시 계산
     * - visitorId가 있으면 그것으로, 없으면 ip+ua 조합으로 생성
     */
    private String computeVisitorHash(String visitorId, HttpServletRequest req) {
        if (visitorId != null && !visitorId.isBlank()) {
            return Hashing.sha256Hex("VID:" + visitorId);
        }
        String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                .map(x -> x.split(",")[0].trim())
                .orElseGet(req::getRemoteAddr);
        String ua = Optional.ofNullable(req.getHeader("User-Agent")).orElse("-");
        return Hashing.sha256Hex("IPUA:" + ip + "|" + ua);
    }

    /**
     * 방문 트래킹
     * - 동일 방문자/동일 날짜(24시간) 내에서는 1회만 카운트
     * - Redis 카운터 증가 + DB VisitEvent 저장(유니크 제약으로 중복 방지)
     */
    public TrackResponseDto track(TrackRequestDto in, HttpServletRequest req) {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime bucket = bucketStart(now);

        String visitorHash = computeVisitorHash(in.getVisitorId(), req);
        String ipHash = Hashing.sha256Hex(Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                .orElse(req.getRemoteAddr()));
        String uaHash = Hashing.sha256Hex(Optional.ofNullable(req.getHeader("User-Agent")).orElse("-"));

        // 12시간 중복 방지 (TTL 2일)
        String dKey = dedupKey(visitorHash, bucket.toInstant());
        Boolean first = redis.opsForValue().setIfAbsent(dKey, "1", Duration.ofHours(48));
        boolean counted = Boolean.TRUE.equals(first);

        if (counted) {
            // 실시간 카운터(일/시간) 증가 + 만료 설정
            String dk = dayKey(now.toLocalDate());
            String hk = hourKey(now.toLocalDate(), now.getHour());
            redis.opsForValue().increment(dk);
            redis.opsForValue().increment(hk);
            redis.expire(dk, Duration.ofDays(60));
            redis.expire(hk, Duration.ofDays(14));

            // DB 기록 (유니크 제약으로 2중 삽입 방지)
            VisitEvent e = VisitEvent.builder()
                    .visitorHash(visitorHash)
                    .ipHash(ipHash)
                    .userAgentHash(uaHash)
                    .visitedAt(now.toInstant())
                    .bucketStart(bucket.toInstant())
                    .day(now.toLocalDate())
                    .hour(now.getHour())
                    .build();
            try {
                repo.save(e);
            } catch (DataIntegrityViolationException ignored) {
                // 레이스로 이미 기록된 경우 충돌 무시
            }
        }

        long today = getTodayCountInternal(now.toLocalDate());
        return TrackResponseDto.builder()
                .counted(counted)
                .todayCount(today)
                .bucketStart(bucket.toInstant())
                .build();
    }

    /** 오늘 카운트 (Redis 우선, 없으면 DB에서 채워 넣음) */
    private long getTodayCountInternal(LocalDate day) {
        String key = dayKey(day);
        String v = redis.opsForValue().get(key);
        if (v != null) return Long.parseLong(v);
        // 캐시에 없으면 DB에서 계산(VisitEvent 한 줄 = 12h 버킷 1회)
        long c = repo.countByDay(day);
        // 캐시에 채우기
        redis.opsForValue().set(key, String.valueOf(c), Duration.ofDays(2));
        return c;
    }

    /** 오늘 방문자수 조회 */
    public TodayCountDto getTodayCount() {
        LocalDate day = LocalDate.now(ZONE);
        return TodayCountDto.builder()
                .date(day.format(ISO_DATE))
                .count(getTodayCountInternal(day))
                .build();
    }

    /** 기간 일별 방문자수 조회 */
    public List<DailyCountDto> getDailyCounts(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) { var t=start; start=end; end=t; }
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : repo.countByDayRange(start, end)) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }
        List<DailyCountDto> out = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long c = map.getOrDefault(d, 0L);
            out.add(DailyCountDto.builder().date(d.format(ISO_DATE)).count(c).build());
        }
        return out;
    }

    /** 특정 날짜 시간별 방문자수 조회 */
    public List<HourlyCountDto> getHourlyCounts(LocalDate day) {
        Map<Integer, Long> map = new HashMap<>();
        for (Object[] row : repo.countByHour(day)) {
            map.put((Integer) row[0], (Long) row[1]);
        }
        List<HourlyCountDto> out = new ArrayList<>();
        for (int h=0; h<24; h++) {
            out.add(HourlyCountDto.builder().hour(h).count(map.getOrDefault(h, 0L)).build());
        }
        return out;
    }
}
