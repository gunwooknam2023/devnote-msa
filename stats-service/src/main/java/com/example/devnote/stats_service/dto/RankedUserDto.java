package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 활동 우수 사용자 랭킹 정보를 최종적으로 반환하기 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedUserDto {
    private long rank;
    private Long id;
    private String name;
    private String picture;
    private Integer activityScore;
}