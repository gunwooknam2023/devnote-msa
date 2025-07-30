package com.example.devnote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FavoriteRequestDto {
    @NotBlank
    private String type;        // YOUTUBE, CHANNEL, NEWS

    @NotBlank
    private String itemId;      // YOUTUBE 영상 ID, 채널 ID, 뉴스 KEY
}
