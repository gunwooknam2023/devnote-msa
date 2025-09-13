package com.example.devnote.stats_service.controller;

import com.example.devnote.stats_service.dto.*;
import com.example.devnote.stats_service.es.RankedSearchTermDto;
import com.example.devnote.stats_service.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// import reactor.core.publisher.Mono; // Mono 사용 안 함

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 랭킹 통계 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stats/ranking")
public class RankingController {

    private final RankingService rankingService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 가장 많이 찜한 콘텐츠 TOP 100 (페이지당 10개)
     */
    @GetMapping("/content-favorites")
    public ResponseEntity<ApiResponseDto<PageResponseDto<RankedContentDto>>> getTopFavoritedContents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponseDto<RankedContentDto> pagedData = rankingService.getTopFavoritedContents(page, size);
        return ResponseEntity.ok(
                ApiResponseDto.<PageResponseDto<RankedContentDto>>builder()
                        .message("Top 50 favorited contents")
                        .statusCode(200)
                        .data(pagedData)
                        .build()
        );
    }

    /**
     * 가장 댓글이 많은 콘텐츠 TOP 100 (페이지당 10개)
     */
    @GetMapping("/content-comments")
    public ResponseEntity<ApiResponseDto<PageResponseDto<RankedContentDto>>> getTopCommentedContents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponseDto<RankedContentDto> pagedData = rankingService.getTopCommentedContents(page, size);
        return ResponseEntity.ok(
                ApiResponseDto.<PageResponseDto<RankedContentDto>>builder()
                        .message("Top 50 commented contents")
                        .statusCode(200)
                        .data(pagedData)
                        .build()
        );
    }

    /**
     * 가장 많이 찜한 채널을 유튜브/뉴스 TOP 10으로 나눠 조회
     */
    @GetMapping("/channel-favorites")
    public ResponseEntity<ApiResponseDto<RankedChannelsDto>> getTopFavoritedChannels() {
        RankedChannelsDto data = rankingService.getTopFavoritedChannelsBySource();
        return ResponseEntity.ok(
                ApiResponseDto.<RankedChannelsDto>builder()
                        .message("Top 10 favorited channels by source")
                        .statusCode(200)
                        .data(data)
                        .build()
        );
    }

    /**
     * 활동 우수 사용자 TOP 10
     */
    @GetMapping("/active-users")
    public ResponseEntity<ApiResponseDto<List<RankedUserDto>>> getTopActiveUsers() {
        List<RankedUserDto> data = rankingService.getTopActiveUsers();
        return ResponseEntity.ok(
                ApiResponseDto.<List<RankedUserDto>>builder()
                        .message("Top 10 active users")
                        .statusCode(200)
                        .data(data)
                        .build()
        );
    }

    /**
     * 실시간 인기 검색어 TOP 10 조회
     */
    @GetMapping("/search-terms")
    public ResponseEntity<ApiResponseDto<List<RankedSearchTermDto>>> getTopSearchTerms() {
        String rankingKey = "ranking:search_terms";

        // Redis의 Sorted Set에서 점수가 높은 순으로 10개 조회
        Set<ZSetOperations.TypedTuple<String>> topTerms = redisTemplate.opsForZSet()
                .reverseRangeWithScores(rankingKey, 0, 9);

        AtomicLong rank = new AtomicLong(1);
        List<RankedSearchTermDto> result = topTerms.stream()
                .map(tuple -> RankedSearchTermDto.builder()
                        .rank(rank.getAndIncrement())
                        .term(tuple.getValue())
                        .score(tuple.getScore())
                        .build())
                .toList();

        return ResponseEntity.ok(
                ApiResponseDto.<List<RankedSearchTermDto>>builder()
                        .message("Top 10 real-time search terms")
                        .statusCode(200)
                        .data(result)
                        .build()
        );
    }
}