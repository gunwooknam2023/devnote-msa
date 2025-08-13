package com.example.devnote.stats_service.repository;

import com.example.devnote.stats_service.entity.VisitEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VisitEventRepository extends JpaRepository<VisitEvent, Long> {

    @Query("select e.day as day, count(e.id) as cnt " +
            "from VisitEvent e " +
            "where e.day between :start and :end " +
            "group by e.day order by e.day asc")
    List<Object[]> countByDayRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select e.hour as hour, count(e.id) as cnt " +
            "from VisitEvent e " +
            "where e.day = :day " +
            "group by e.hour order by e.hour asc")
    List<Object[]> countByHour(@Param("day") LocalDate day);

    long countByDay(LocalDate day);
}
