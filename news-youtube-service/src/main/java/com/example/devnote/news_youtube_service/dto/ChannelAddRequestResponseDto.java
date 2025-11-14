package com.example.devnote.news_youtube_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelAddRequestResponseDto {
    private Long id;
    private String channelName;
    private String link;
    private String source;
    private String requestReason;
    private Instant createdAt;
}