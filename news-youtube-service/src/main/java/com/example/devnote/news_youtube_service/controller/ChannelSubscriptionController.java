package com.example.devnote.news_youtube_service.controller;

import com.example.devnote.news_youtube_service.config.KafkaProducerConfig;
import com.example.devnote.news_youtube_service.dto.ApiResponseDto;
import com.example.devnote.news_youtube_service.dto.ChannelSubscriptionRequestDto;
import com.example.devnote.news_youtube_service.dto.ChannelSubscriptionSummaryDto;
import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import com.example.devnote.news_youtube_service.service.ChannelSubscriptionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
public class ChannelSubscriptionController {
    private final ChannelSubscriptionRepository channelSubscriptionRepository;
    private final ChannelSubscriptionQueryService channelSubscriptionQueryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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

    /** 단일 채널 조회 (찜 기능용 존재 확인) */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ChannelSubscription>> getById(
            @PathVariable Long id
    ) {
        ChannelSubscription sub = channelSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "ChannelSubscription not found: " + id
                ));

        return ResponseEntity.ok(
                ApiResponseDto.<ChannelSubscription>builder()
                        .message("Fetched channel")
                        .statusCode(HttpStatus.OK.value())
                        .data(sub)
                        .build()
        );
    }

    /**
     * 채널 구독 정보 삭제 + Kafka로 삭제 이벤트 발행
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteChannel(@PathVariable Long id) {
        ChannelSubscription sub = channelSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ChannelSubscription not found: " + id));

        // 1) DB에서 삭제
        channelSubscriptionRepository.delete(sub);

        // 2) Kafka로 삭제 이벤트 전송 (key: 채널 ID)
        kafkaTemplate.send(
                KafkaProducerConfig.TOPIC_CHANNEL_DELETED,
                String.valueOf(id)
        );

        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("Channel deleted and event published")
                        .statusCode(HttpStatus.OK.value())
                        .data(null)
                        .build()
        );
    }

    /**
     * 전체 유튜브 채널 구독 현황
     */
    @GetMapping("/youtube/subscriptions")
    public ResponseEntity<ApiResponseDto<List<ChannelSubscriptionSummaryDto>>> getYoutubeSubscriptions() {
        List<ChannelSubscriptionSummaryDto> data =
                channelSubscriptionQueryService.getSubscriptionsBySource("YOUTUBE");

        return ResponseEntity.ok(
                ApiResponseDto.<List<ChannelSubscriptionSummaryDto>>builder()
                        .message("Fetched youtube subscriptions")
                        .statusCode(200)
                        .data(data)
                        .build()
        );
    }

    /**
     * 전체 뉴스 채널 구독 현황
     */
    @GetMapping("/news/subscriptions")
    public ResponseEntity<ApiResponseDto<List<ChannelSubscriptionSummaryDto>>> getNewsSubscriptions() {
        List<ChannelSubscriptionSummaryDto> data =
                channelSubscriptionQueryService.getSubscriptionsBySource("NEWS");

        return ResponseEntity.ok(
                ApiResponseDto.<List<ChannelSubscriptionSummaryDto>>builder()
                        .message("Fetched news subscriptions")
                        .statusCode(200)
                        .data(data)
                        .build()
        );
    }
}
