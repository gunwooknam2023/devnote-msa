package com.example.devnote.stats_service.dto.internal;

import lombok.Data;

/**
 * user-service로부터 콘텐츠 랭킹 ID 목록을 받아오기 위한 DTO
 */
@Data
public class RankedContentIdDto {
    private Long contentId;
    private long count;
}