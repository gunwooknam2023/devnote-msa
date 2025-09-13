package com.example.devnote.stats_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 유튜브/뉴스 채널 랭킹을 함께 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankedChannelsDto {
    private List<RankedChannelDto> youtube;
    private List<RankedChannelDto> news;
}