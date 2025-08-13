package com.example.devnote.stats_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "traffic_minute",
        uniqueConstraints = @UniqueConstraint(
                name="uk_traffic_min",
                columnNames={"day","hour","minute","method","path"}
        ),
        indexes = {
                @Index(name="idx_traffic_day", columnList = "day"),
                @Index(name="idx_traffic_hour", columnList = "day,hour"),
                @Index(name="idx_traffic_path", columnList = "path")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrafficMinute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private LocalDate day;    // Asia/Seoul

    @Column(nullable=false)
    private int hour;         // 0..23

    @Column(nullable=false)
    private int minute;       // 0..59

    @Column(nullable=false, length = 10)
    private String method;    // GET/POST/...

    @Column(nullable=false)
    private String path;      // 요청 경로 (쿼리스트링 제외)

    @Column(nullable=false)
    private long count;       // 해당 분의 요청 수
}