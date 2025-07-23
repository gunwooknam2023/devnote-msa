package com.example.devnote.processor_service.controller;

import com.example.devnote.processor_service.dto.ApiResponseDto;
import com.example.devnote.processor_service.dto.CommentRequestDto;
import com.example.devnote.processor_service.dto.CommentResponseDto;
import com.example.devnote.processor_service.dto.CommentUpdateDto;
import com.example.devnote.processor_service.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 댓글 API
 * POST   /api/v1/comments          : 댓글 생성
 * GET    /api/v1/comments         : 특정 콘텐츠 댓글 전체 조회
 * PUT    /api/v1/comments/{id}     : 댓글 수정
 * DELETE /api/v1/comments/{id}     : 댓글 삭제
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
@Slf4j
public class CommentController {
    private final CommentService commentService;

    /** 댓글 생성 */
    @PostMapping
    public ResponseEntity<ApiResponseDto<CommentResponseDto>> create(
            @Valid @RequestBody CommentRequestDto req) {
        log.info("POST /api/v1/comments {}", req);
        CommentResponseDto dto = commentService.createComment(req);
        return ResponseEntity.ok(
                ApiResponseDto.<CommentResponseDto>builder()
                        .message("Comment created")
                        .statusCode(201)
                        .data(dto)
                        .build()
        );
    }

    /** 특정 콘텐츠 댓글 전체 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<CommentResponseDto>>> list(
            @RequestParam Long contentId) {
        log.info("GET /api/v1/comments?contentId={}", contentId);
        List<CommentResponseDto> comments = commentService.listCommentsByContent(contentId);
        return ResponseEntity.ok(
                ApiResponseDto.<List<CommentResponseDto>>builder()
                        .message("Fetched comments")
                        .statusCode(200)
                        .data(comments)
                        .build()
        );
    }

    /** 댓글 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CommentResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateDto req) {
        log.info("PUT /api/v1/comments/{} {}", id, req);
        CommentResponseDto dto = commentService.updateComment(id, req);
        return ResponseEntity.ok(
                ApiResponseDto.<CommentResponseDto>builder()
                        .message("Comment updated")
                        .statusCode(200)
                        .data(dto)
                        .build()
        );
    }

    /** 댓글 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @PathVariable Long id,
            @RequestParam String password) {
        log.info("DELETE /api/v1/comments/{} password=****", id);
        commentService.deleteComment(id, password);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("Comment deleted")
                        .statusCode(200)
                        .build()
        );
    }
}
