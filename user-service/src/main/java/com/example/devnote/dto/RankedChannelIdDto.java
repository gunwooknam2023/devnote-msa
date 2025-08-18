package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채널 랭킹 집계 결과를 담는 DTO
 * (내부 서비스 통신용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankedChannelIdDto {
    private Long channelId; // 채널 ID
    private long count;     // 집계된 찜 수
}