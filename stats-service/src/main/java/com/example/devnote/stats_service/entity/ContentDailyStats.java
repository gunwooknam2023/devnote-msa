package com.example.devnote.stats_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "content_daily_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_content_stats_day", columnNames = {"day"}),
        indexes = @Index(name = "idx_content_stats_day", columnList = "day"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDailyStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 통계 기준일 */
    @Column(nullable = false)
    private LocalDate day;

    /** 해당 일의 신규 콘텐츠 수 */
    @Column(nullable = false)
    private long count;
}
