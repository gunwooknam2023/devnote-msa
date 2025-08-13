package com.example.devnote.stats_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "visit_event",
        uniqueConstraints = @UniqueConstraint(name="uk_visitor_bucket", columnNames = {"visitor_hash", "bucket_start"}),
        indexes = {
                @Index(name="idx_visit_day", columnList = "day"),
                @Index(name="idx_visit_hour", columnList = "day,hour"),
                @Index(name="idx_bucket_start", columnList = "bucket_start")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="visitor_hash", nullable=false, length=64)
    private String visitorHash;

    @Column(name="ip_hash", length=64)
    private String ipHash;

    @Column(name="ua_hash", length=64)
    private String userAgentHash;

    @Column(name="visited_at", nullable=false)
    private Instant visitedAt;

    @Column(name="bucket_start", nullable=false)
    private Instant bucketStart; // 12시간 버킷 시작 (Asia/Seoul 기준)

    @Column(name="day", nullable=false)
    private LocalDate day;       // Asia/Seoul 날짜

    @Column(name="hour", nullable=false)
    private int hour;            // 0..23 (Asia/Seoul 기준)
}
