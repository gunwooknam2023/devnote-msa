package com.example.devnote.controller;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.PostListResponseDto;
import com.example.devnote.service.PostScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostScrapController {

    private final PostScrapService postScrapService;

    /**
     * 게시글 스크랩/스크랩 취소
     */
    @PostMapping("/{postId}/scrap")
    public ResponseEntity<ApiResponseDto<Void>> toggleScrap(@PathVariable Long postId) {
        postScrapService.toggleScrap(postId);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .message("스크랩이 처리되었습니다.")
                .statusCode(200)
                .build());
    }

    /**
     * 사용자의 스크랩한 게시글 목록 조회
     */
    @GetMapping("/scraped")
    public ResponseEntity<ApiResponseDto<Page<PostListResponseDto>>> getScrapedPosts(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<PostListResponseDto> scrapedPosts = postScrapService.getScrapedPosts(pageable);
        return ResponseEntity.ok(ApiResponseDto.<Page<PostListResponseDto>>builder()
                .message("스크랩한 게시글 목록을 조회했습니다.")
                .statusCode(200)
                .data(scrapedPosts)
                .build());
    }
}
