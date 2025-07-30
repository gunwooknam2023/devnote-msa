package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FavoriteResponseDto {
    private Long id;
    private String type;    // YOUTUBE, CHANNEL, NEWS
    private String itemId;  // YOUTUBE 영상 ID, 채널 ID, 뉴스 KEY
}
