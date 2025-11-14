package com.example.devnote.news_youtube_service.controller;

import com.example.devnote.news_youtube_service.dto.ApiResponseDto;
import com.example.devnote.news_youtube_service.dto.ChannelAddRequestDto;
import com.example.devnote.news_youtube_service.dto.ChannelAddRequestResponseDto;
import com.example.devnote.news_youtube_service.service.ChannelAddRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/requests")
public class ChannelAddRequestController {

    private final ChannelAddRequestService channelAddRequestService;

    /**
     * 채널 추가 요청 (인증 불필요)
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto<ChannelAddRequestResponseDto>> createRequest(
            @Validated @RequestBody ChannelAddRequestDto dto) {
        ChannelAddRequestResponseDto responseDto = channelAddRequestService.createRequest(dto);

        return ResponseEntity.status(201).body(
                ApiResponseDto.<ChannelAddRequestResponseDto>builder()
                        .message("채널 추가 요청이 접수되었습니다.")
                        .statusCode(201)
                        .data(responseDto)
                        .build()
        );
    }
}