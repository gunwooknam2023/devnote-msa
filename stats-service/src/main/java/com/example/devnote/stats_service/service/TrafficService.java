package com.example.devnote.stats_service.service;

import com.example.devnote.stats_service.dto.DailyCountDto;
import com.example.devnote.stats_service.dto.HourlyCountDto;
import com.example.devnote.stats_service.dto.TodayCountDto;
import com.example.devnote.stats_service.dto.TrafficTrackResponseDto;
import com.example.devnote.stats_service.repository.TrafficMinuteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 실시간 트래픽(요청 수) 서비스
 * - 요청마다 1건 카운트 (중복 제거 없음)
 * - Redis에 일/시간/분 카운터 증가 (분 키는 method/path까지 구분)
 * - 1분마다 Redis의 분 키를 DB(traffic_minute)에 upsert 하여 영속
 * - 조회는 오늘은 Redis 포함, 과거는 DB 합산
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrafficService {

    private final StringRedisTemplate redis;
    private final TrafficMinuteRepository repo;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    // Redis 플러시 후보 Set
    private static final String PENDING_SET = "traffic:minute:pending";

    // Redis 키 포맷
    private String dayKey(LocalDate day) {
        return "traffic:count:day:" + DAY_FMT.format(day);
    }

    private String hourKey(LocalDate day, int hour) {
        return "traffic:count:hour:" + DAY_FMT.format(day) + ":" + String.format("%02d", hour);
    }

    /** 집계 전체 통계 */
    private String minuteKey(LocalDate day, int hour, int minute) {
        return "traffic:count:minute:" + DAY_FMT.format(day)
                + ":" + String.format("%02d", hour)
                + ":" + String.format("%02d", minute);
    }

    /** 메서드/경로 세분화 분 키 */
    private String minuteKeyDim(LocalDate day, int hour, int minute, String method, String path) {
        String m = method.toUpperCase(Locale.ROOT);
        String p64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(path.getBytes(StandardCharsets.UTF_8));
        return "traffic:count:minute:" + DAY_FMT.format(day)
                + ":" + String.format("%02d", hour)
                + ":" + String.format("%02d", minute)
                + ":" + m
                + ":" + p64;
    }

    /**
     * 요청 1건 트래킹
     * - 게이트웨이에서 method/path를 전달
     * - Redis 카운터 증가(+ TTL) 후, 분 키를 플러시 후보 Set에 추가
     */
    public TrafficTrackResponseDto trackOnce(String method, String path) {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        LocalDate day = now.toLocalDate();
        int hour = now.getHour();
        int minute = now.getMinute();

        // 실시간 전체 합계 카운터 증분 (오늘/시간/분)
        String dk = dayKey(day);
        String hk = hourKey(day, hour);
        String mk = minuteKey(day, hour, minute);
        redis.opsForValue().increment(dk);
        redis.opsForValue().increment(hk);
        redis.opsForValue().increment(mk);
        redis.expire(dk, Duration.ofDays(60));
        redis.expire(hk, Duration.ofDays(14));
        redis.expire(mk, Duration.ofDays(2));

        // 메서드/경로 구분 분 카운터 증분 + 플러시 후보 등록
        String mkDim = minuteKeyDim(day, hour, minute, method, path);
        redis.opsForValue().increment(mkDim);
        redis.expire(mkDim, Duration.ofDays(2));
        redis.opsForSet().add(PENDING_SET, mkDim);

        return TrafficTrackResponseDto.builder()
                .counted(true)
                .todayCount(getTodayCountInternal(day))
                .build();
    }

    /** 오늘 총 요청 수 (Redis 우선, 없으면 DB) */
    public TodayCountDto getToday() {
        LocalDate day = LocalDate.now(ZONE);
        return TodayCountDto.builder()
                .date(day.format(ISO_DATE))
                .count(getTodayCountInternal(day))
                .build();
    }

    /** 오늘 총 요청 수 */
    private long getTodayCountInternal(LocalDate day) {
        String v = redis.opsForValue().get(dayKey(day));
        if (v != null) return Long.parseLong(v);

        long db = repo.sumToday(day);
        redis.opsForValue().set(dayKey(day), String.valueOf(db), Duration.ofDays(2));
        return db;
    }

    /** 기간 일별 요청 수 (오늘은 Redis 포함, 과거는 DB 합산) */
    public List<DailyCountDto> getDaily(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            LocalDate t = start;
            start = end;
            end = t;
        }

        Map<LocalDate, Long> map = new HashMap<>();
        List<Object[]> rows = repo.sumByDayRange(start, end);
        for (Object[] row : rows) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }

        List<DailyCountDto> out = new ArrayList<>();
        LocalDate today = LocalDate.now(ZONE);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            long c = d.equals(today) ? getTodayCountInternal(d) : map.getOrDefault(d, 0L);
            out.add(DailyCountDto.builder()
                    .date(d.format(ISO_DATE))
                    .count(c)
                    .build());
        }
        return out;
    }

    /** 특정 날짜 시간별 요청 수 (오늘의 현재 시간은 Redis 값으로 보정) */
    public List<HourlyCountDto> getHourly(LocalDate day) {
        Map<Integer, Long> map = new HashMap<>();
        List<Object[]> rows = repo.sumByHour(day);
        for (Object[] row : rows) {
            map.put((Integer) row[0], (Long) row[1]);
        }

        // 오늘의 현재 진행 중 시간은 Redis 값으로 보정
        if (day.equals(LocalDate.now(ZONE))) {
            ZonedDateTime now = ZonedDateTime.now(ZONE);
            int thisHour = now.getHour();
            String hk = hourKey(day, thisHour);
            String v = redis.opsForValue().get(hk);
            if (v != null) map.put(thisHour, Long.parseLong(v));
        }

        List<HourlyCountDto> out = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            out.add(HourlyCountDto.builder()
                    .hour(h)
                    .count(map.getOrDefault(h, 0L))
                    .build());
        }
        return out;
    }

    /**
     * 1분마다 Redis의 분 키(메서드/경로 포함)를 DB에 업서트
     * - 키 형식: traffic:count:minute:yyyyMMdd:HH:mm:METHOD:BASE64PATH
     * - 처리 성공 시 해당 키를 삭제하고, 후보 Set에서도 제거
     */
    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void flushMinuteCounters() {
        Set<String> keys = redis.opsForSet().members(PENDING_SET);
        if (keys == null || keys.isEmpty()) return;

        int size = keys.size();
        log.info("[Traffic][FLUSH] pending={}", size);

        for (String key : keys) {
            try {
                String[] parts = key.split(":");
                if (parts.length < 8) {
                    log.warn("[Traffic][FLUSH] skip invalid key={}", key);
                    redis.opsForSet().remove(PENDING_SET, key);
                    continue;
                }

                String ymd = parts[3];
                int hour = Integer.parseInt(parts[4]);
                int minute = Integer.parseInt(parts[5]);
                String method = parts[6];
                String base64Path = parts[7];
                String path = new String(Base64.getUrlDecoder().decode(base64Path), StandardCharsets.UTF_8);
                if (path.length() > 255) path = path.substring(0, 255);

                // Lua로 원자적 GET+DEL
                String val = getAndDeleteAtomic(key);
                if (val == null) {
                    // 이미 처리되었거나 값이 없다면 후보에서 제거
                    redis.opsForSet().remove(PENDING_SET, key);
                    continue;
                }
                long delta = Long.parseLong(val);
                LocalDate day = LocalDate.parse(ymd, DAY_FMT);

                repo.upsertMinute(day, hour, minute, method.toUpperCase(Locale.ROOT), path, delta);

                // 후보 set에서 제거
                redis.opsForSet().remove(PENDING_SET, key);

            } catch (Exception e) {
                log.warn("[Traffic][FLUSH] failed for key={} : {}", key, e.toString());
            }
        }
    }

    /**
     * Redis에서 Lua(EVAL)로 GET → DEL을 원자적으로 수행 후 값을 반환
     * - 값이 없으면 null
     */
    private String getAndDeleteAtomic(String key) {
        return redis.execute((RedisCallback<String>) connection -> {
            String script = ""
                    + "local v = redis.call('GET', KEYS[1]); "
                    + "if v then redis.call('DEL', KEYS[1]); end; "
                    + "return v;";

            byte[] rawScript = script.getBytes(StandardCharsets.UTF_8);
            byte[] rawKey = redis.getStringSerializer().serialize(key);
            if (rawKey == null) {
                return null;
            }

            Object evalResult = connection.eval(rawScript, ReturnType.VALUE, 1, rawKey);
            if (evalResult == null) return null;

            if (evalResult instanceof byte[]) {
                return new String((byte[]) evalResult, StandardCharsets.UTF_8);
            }
            if (evalResult instanceof String) {
                return (String) evalResult;
            }
            return String.valueOf(evalResult);
        });
    }
}