package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 콘텐츠의 통계(찜/댓글) 변경을 알리기 위한 Kafka 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentStatsUpdateDto {
    private Long contentId;
    private int favoriteDelta; // 찜 증감 (+1 또는 -1)
    private int commentDelta;  // 댓글 증감 (+1 또는 -1)
}
