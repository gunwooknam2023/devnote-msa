package com.example.devnote.stats_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "page_duration_daily_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_duration_stats_day", columnNames = {"day"}),
        indexes = @Index(name = "idx_duration_stats_day", columnList = "day"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageDurationDailyStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 통계 기준일 */
    @Column(nullable = false)
    private LocalDate day;

    /** 해당 일의 총 체류 시간 (단위: 초) */
    @Column(nullable = false)
    private long totalDurationSeconds;
}