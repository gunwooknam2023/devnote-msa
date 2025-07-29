package com.example.devnote.news_youtube_service.controller;

import com.example.devnote.news_youtube_service.dto.ApiResponseDto;
import com.example.devnote.news_youtube_service.dto.ChannelSubscriptionRequestDto;
import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
public class ChannelSubscriptionController {
    private final ChannelSubscriptionRepository channelSubscriptionRepository;

    /** 신규 채널 등록 (초기Loaded=false) */
    @PostMapping
    public ResponseEntity<ApiResponseDto<ChannelSubscription>> add(
            @Validated @RequestBody ChannelSubscriptionRequestDto req) {
        ChannelSubscription sub = ChannelSubscription.builder()
                .youtubeName(req.getYoutubeName())
                .channelId(req.getChannelId())
                .initialLoaded(false)
                .build();
        sub = channelSubscriptionRepository.save(sub);
        return ResponseEntity.status(201).body(
                ApiResponseDto.<ChannelSubscription>builder()
                        .message("Channel registered")
                        .statusCode(201)
                        .data(sub)
                        .build()
        );
    }

    /** 전체 구독 채널 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ChannelSubscription>>> list() {
        List<ChannelSubscription> list = channelSubscriptionRepository.findAll();
        return ResponseEntity.ok(
                ApiResponseDto.<List<ChannelSubscription>>builder()
                        .message("Fetched subscriptions")
                        .statusCode(200)
                        .data(list)
                        .build()
        );
    }
}
