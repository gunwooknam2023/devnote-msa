package com.example.devnote.stats_service.controller;

import com.example.devnote.stats_service.dto.*;
import com.example.devnote.stats_service.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// import reactor.core.publisher.Mono; // Mono 사용 안 함

import java.util.List;

/**
 * 랭킹 통계 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stats/ranking")
public class RankingController {

    private final RankingService rankingService;

    /**
     * 가장 많이 찜한 콘텐츠 TOP 50 (페이지당 10개)
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
     * 가장 댓글이 많은 콘텐츠 TOP 50 (페이지당 10개)
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
     * 가장 많이 찜한 채널 TOP 10
     */
    @GetMapping("/channel-favorites")
    public ResponseEntity<ApiResponseDto<List<RankedChannelDto>>> getTopFavoritedChannels() {
        List<RankedChannelDto> data = rankingService.getTopFavoritedChannels();
        return ResponseEntity.ok(
                ApiResponseDto.<List<RankedChannelDto>>builder()
                        .message("Top 10 favorited channels")
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
}