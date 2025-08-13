package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCountDto {
    private String date; // yyyy-MM-dd
    private long count;  // 특정일 방문자수
}