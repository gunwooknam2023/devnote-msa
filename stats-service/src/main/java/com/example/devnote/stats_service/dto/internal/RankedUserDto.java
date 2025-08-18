package com.example.devnote.stats_service.dto.internal;

import lombok.Data;

/**
 * user-service로부터 활동 우수 사용자 목록을 받아오기 위한 DTO
 */
@Data
public class RankedUserDto {
    private Long id;
    private String name;
    private String picture;
    private Integer activityScore;
}