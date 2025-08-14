package com.example.devnote.stats_service.repository;

import com.example.devnote.stats_service.entity.ContentDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContentDailyStatsRepository extends JpaRepository<ContentDailyStats, Long> {

    /** 기간 내 일별 데이터 조회 */
    @Query("SELECT c.day as day, c.count as count FROM ContentDailyStats c WHERE c.day BETWEEN :start AND :end ORDER BY c.day ASC")
    List<Object[]> findDailyCountsByRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 특정 연도의 월별 합계 조회 */
    @Query("SELECT FUNCTION('MONTH', c.day) as month, SUM(c.count) as count " +
            "FROM ContentDailyStats c " +
            "WHERE FUNCTION('YEAR', c.day) = :year " +
            "GROUP BY FUNCTION('MONTH', c.day) " +
            "ORDER BY FUNCTION('MONTH', c.day) ASC")
    List<Object[]> findMonthlyCountsByYear(@Param("year") int year);

    /** 기간 내 연도별 합계 조회 */
    @Query("SELECT FUNCTION('YEAR', c.day) as year, SUM(c.count) as count " +
            "FROM ContentDailyStats c " +
            "WHERE FUNCTION('YEAR', c.day) BETWEEN :startYear AND :endYear " +
            "GROUP BY FUNCTION('YEAR', c.day) " +
            "ORDER BY FUNCTION('YEAR', c.day) ASC")
    List<Object[]> findYearlyCountsByRange(@Param("startYear") int startYear, @Param("endYear") int endYear);

    Optional<ContentDailyStats> findByDay(LocalDate day);
}