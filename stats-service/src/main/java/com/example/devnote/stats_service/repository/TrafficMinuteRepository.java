package com.example.devnote.stats_service.repository;

import com.example.devnote.stats_service.entity.TrafficMinute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TrafficMinuteRepository extends JpaRepository<TrafficMinute, Long> {

    // 일별 합계(기간)
    @Query("select t.day as day, sum(t.count) as cnt " +
            "from TrafficMinute t " +
            "where t.day between :start and :end " +
            "group by t.day order by t.day asc")
    List<Object[]> sumByDayRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // 시간별 합계(하루)
    @Query("select t.hour as hour, sum(t.count) as cnt " +
            "from TrafficMinute t " +
            "where t.day = :day " +
            "group by t.hour order by t.hour asc")
    List<Object[]> sumByHour(@Param("day") LocalDate day);

    // 오늘 합계(빠른 조회용)
    @Query("select coalesce(sum(t.count),0) from TrafficMinute t where t.day = :day")
    long sumToday(@Param("day") LocalDate day);

    // MariaDB upsert (분 집계 병합)
    @Modifying
    @Query(value = """
    INSERT INTO traffic_minute(day, hour, minute, method, path, count)
    VALUES (:day, :hour, :minute, :method, :path, :delta)
    ON DUPLICATE KEY UPDATE count = count + VALUES(count)
    """, nativeQuery = true)
    void upsertMinute(@Param("day") LocalDate day,
                      @Param("hour") int hour,
                      @Param("minute") int minute,
                      @Param("method") String method,
                      @Param("path") String path,
                      @Param("delta") long delta);

    /** 특정 날짜의 콘텐츠 조회수(뉴스/유튜브) 합계를 조회 */
    @Query("SELECT COALESCE(SUM(t.count), 0) FROM TrafficMinute t " +
            "WHERE t.day = :date AND t.method = 'GET' " +
            "AND (t.path LIKE '/api/v1/contents/%' OR t.path LIKE '/r/%')")
    long sumContentViewsByDay(@Param("date") LocalDate date);
}
