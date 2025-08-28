package com.example.devnote.stats_service.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedSearchTermDto {
    private long rank;      // 순위
    private String term;    // 검색어
    private double score;   // 가중치가 적용된 점수
}
