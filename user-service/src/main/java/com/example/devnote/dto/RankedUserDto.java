package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 활동 우수 사용자 랭킹 정보를 담는 DTO
 * (내부 서비스 통신용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedUserDto {
    private Long id;
    private String name;
    private String picture;
    private Integer activityScore;
}
