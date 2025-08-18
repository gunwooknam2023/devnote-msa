package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 콘텐츠 랭킹 집계 결과를 담는 DTO
 * (내부 서비스 통신용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankedContentIdDto {
    private Long contentId; // 콘텐츠 ID
    private long count;     // 집계된 수 (찜 또는 댓글 수)
}