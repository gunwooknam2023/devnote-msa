package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyCountDto {
    private int hour;   // 0..23
    private long count; // 해당 시각 방문자수
}