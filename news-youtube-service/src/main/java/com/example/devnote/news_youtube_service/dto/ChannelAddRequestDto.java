package com.example.devnote.news_youtube_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChannelAddRequestDto {
    /** 요청한 채널명 (유튜브 채널명 또는 언론사명) */
    @NotBlank(message = "채널명을 입력해주세요.")
    @Size(max = 100, message = "채널명은 100자를 초과할 수 없습니다.")
    private String channelName;

    /** 채널 링크 (유튜브 채널 URL 또는 뉴스 언론사 URL, 선택사항) */
    @Size(max = 512, message = "링크는 512자를 초과할 수 없습니다.")
    private String link;

    /** 소스 구분 ("YOUTUBE" 또는 "NEWS") */
    @NotBlank(message = "소스 구분을 선택해주세요.")
    private String source;

    /** 채널 추가 요청 사유 (선택사항) */
    @Size(max = 1000, message = "추가 사유는 1000자를 초과할 수 없습니다.")
    private String requestReason;
}