package com.example.devnote.controller;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.entity.enums.VoteType;
import com.example.devnote.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts/{postId}")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    /**
     * 게시글에 '좋아요'
     * 이미 누른 상태라면 '좋아요'를 취소
     */
    @PostMapping("/like")
    public ResponseEntity<ApiResponseDto<Void>> likePost(@PathVariable Long postId) {
        postLikeService.vote(postId, VoteType.LIKE);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder().message("처리되었습니다.").statusCode(200).build());
    }

    /**
     * 게시글에 '싫어요'
     * 이미 누른 상태라면 '싫어요'를 취소
     */
    @PostMapping("/dislike")
    public ResponseEntity<ApiResponseDto<Void>> dislikePost(@PathVariable Long postId) {
        postLikeService.vote(postId, VoteType.DISLIKE);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder().message("처리되었습니다.").statusCode(200).build());
    }
}