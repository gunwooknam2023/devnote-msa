package com.example.devnote.news_youtube_service.controller;

import com.example.devnote.news_youtube_service.dto.ApiResponseDto;
import com.example.devnote.news_youtube_service.service.NewsFetchService;
import com.example.devnote.news_youtube_service.service.YouTubeFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 수동 트리거용 REST API
 * - /api/v1/fetch/news     : 뉴스 즉시 수집
 * - /api/v1/fetch/youtube  : 유튜브 즉시 수집
 * - /api/v1/fetch/health   : 헬스체크
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/fetch")
public class ContentController {
    private final NewsFetchService newsService;
    private final YouTubeFetchService youtubeService;

    /** 뉴스 수집 → Kafka 발행 트리거 */
    @PostMapping("/news")
    public ResponseEntity<ApiResponseDto<Void>> fetchNews() {
        log.info("API /fetch/news called");
        newsService.fetchAndPublishNews();
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("News fetch triggered")
                        .statusCode(200)
                        .data(null)
                        .build()
        );
    }

    /** 유튜브 수집 → Kafka 발행 트리거 */
    @PostMapping("/youtube")
    public ResponseEntity<ApiResponseDto<Void>> fetchYoutube() {
        log.info("API /fetch/youtube called");
        youtubeService.fetchAndPublishYoutube();
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("YouTube fetch triggered")
                        .statusCode(200)
                        .data(null)
                        .build()
        );
    }

    /** 헬스 체크: 서비스 정상 여부 반환 */
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<String>> health() {
        log.info("API /fetch/health called");
        return ResponseEntity.ok(
                ApiResponseDto.<String>builder()
                        .message("OK")
                        .statusCode(200)
                        .data("service is up")
                        .build()
        );
    }
}
