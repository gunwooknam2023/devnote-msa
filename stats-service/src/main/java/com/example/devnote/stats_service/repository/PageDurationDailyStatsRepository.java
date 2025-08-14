package com.example.devnote.stats_service.repository;

import com.example.devnote.stats_service.entity.PageDurationDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PageDurationDailyStatsRepository extends JpaRepository<PageDurationDailyStats, Long> {

    /** 기간 내 일별 데이터 조회 */
    @Query("SELECT p.day as day, p.totalDurationSeconds as count FROM PageDurationDailyStats p WHERE p.day BETWEEN :start AND :end ORDER BY p.day ASC")
    List<Object[]> findDailyDurationsByRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 특정 연도의 월별 합계 조회 */
    @Query("SELECT FUNCTION('MONTH', p.day) as month, SUM(p.totalDurationSeconds) as count " +
            "FROM PageDurationDailyStats p " +
            "WHERE FUNCTION('YEAR', p.day) = :year " +
            "GROUP BY FUNCTION('MONTH', p.day) " +
            "ORDER BY FUNCTION('MONTH', p.day) ASC")
    List<Object[]> findMonthlyDurationsByYear(@Param("year") int year);

    /** 기간 내 연도별 합계 조회 */
    @Query("SELECT FUNCTION('YEAR', p.day) as year, SUM(p.totalDurationSeconds) as count " +
            "FROM PageDurationDailyStats p " +
            "WHERE FUNCTION('YEAR', p.day) BETWEEN :startYear AND :endYear " +
            "GROUP BY FUNCTION('YEAR', p.day) " +
            "ORDER BY FUNCTION('YEAR', p.day) ASC")
    List<Object[]> findYearlyDurationsByRange(@Param("startYear") int startYear, @Param("endYear") int endYear);

    Optional<PageDurationDailyStats> findByDay(LocalDate day);
}
