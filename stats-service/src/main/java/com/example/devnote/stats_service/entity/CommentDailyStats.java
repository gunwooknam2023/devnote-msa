package com.example.devnote.stats_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "comment_daily_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_stats_day", columnNames = {"day"}),
        indexes = @Index(name = "idx_comment_stats_day", columnList = "day"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDailyStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 통계 기준일 */
    @Column(nullable = false)
    private LocalDate day;

    /** 해당 일의 신규 댓글 수 */
    @Column(nullable = false)
    private long count;
}