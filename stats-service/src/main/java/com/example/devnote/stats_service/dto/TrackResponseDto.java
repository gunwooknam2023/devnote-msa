package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackResponseDto {
    private boolean counted;     // 이번 요청으로 카운트가 실제 증가했는지
    private long todayCount;     // 오늘 총 방문자수(12h 버킷 기준 합산)
    private Instant bucketStart; // 적용된 12h 버킷 시작 시각
}