package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 유저가 찜한 채널 & 콘텐츠를 한 번에 묶어서 반환하는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoritesResponseDto {
    /** 찜한 유튜브 채널 목록 */
    private List<ChannelSubscriptionDto> channels;

    /** 찜한 콘텐츠(영상/뉴스) 목록 */
    private List<ContentDto> contents;
}
