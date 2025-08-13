package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficTrackResponseDto {
    private boolean counted; // 항상 true (요청 = 1건)
    private long todayCount; // 금일 총 요청 수 (실시간)
}
