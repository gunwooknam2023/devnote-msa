package com.example.devnote.controller;

import com.example.devnote.dto.RankedChannelIdDto;
import com.example.devnote.dto.RankedContentIdDto;
import com.example.devnote.dto.RankedUserDto;
import com.example.devnote.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 서비스 간 통신(내부용)을 위한 랭킹 데이터 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/ranking")
public class InternalRankingController {

    private final RankingService rankingService;

    /**
     * 가장 많이 찜한 콘텐츠 ID 목록
     */
    @GetMapping("/content-favorites")
    public ResponseEntity<Page<RankedContentIdDto>> getTopFavoritedContents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<RankedContentIdDto> result = rankingService.getTopFavoritedContents(PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    /**
     * 가장 댓글이 많은 콘텐츠 ID 목록
     */
    @GetMapping("/content-comments")
    public ResponseEntity<Page<RankedContentIdDto>> getTopCommentedContents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<RankedContentIdDto> result = rankingService.getTopCommentedContents(PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    /**
     * 가장 많이 찜한 채널 ID 목록
     */
    @GetMapping("/channel-favorites")
    public ResponseEntity<List<RankedChannelIdDto>> getTopFavoritedChannels() {
        // 상위 10개만 조회
        List<RankedChannelIdDto> result = rankingService.getTopFavoritedChannels(PageRequest.of(0, 10));
        return ResponseEntity.ok(result);
    }

    /**
     * 활동 점수가 높은 사용자 TOP 10 목록
     */
    @GetMapping("/active-users")
    public ResponseEntity<List<RankedUserDto>> getTopActiveUsers() {
        List<RankedUserDto> result = rankingService.getTopActiveUsers();
        return ResponseEntity.ok(result);
    }
}