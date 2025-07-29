package com.example.devnote.news_youtube_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChannelSubscriptionRequestDto {
    @NotBlank(message = "유튜브명을 입력해주세요.")
    private String youtubeName;

    @NotBlank(message = "채널ID를 입력해주세요.")
    private String channelId;
}
