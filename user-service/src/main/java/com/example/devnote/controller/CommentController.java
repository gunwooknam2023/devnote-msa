package com.example.devnote.controller;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.CommentRequestDto;
import com.example.devnote.dto.CommentResponseDto;
import com.example.devnote.dto.CommentUpdateDto;
import com.example.devnote.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    /** 댓글 생성 */
    @PostMapping
    public ResponseEntity<ApiResponseDto<CommentResponseDto>> create(
            @Valid @RequestBody CommentRequestDto req) {
        CommentResponseDto dto = commentService.createComment(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.<CommentResponseDto>builder()
                        .message("Comment created")
                        .statusCode(HttpStatus.CREATED.value())
                        .data(dto)
                        .build());
    }

    /** 특정 콘텐츠 댓글 전체 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<CommentResponseDto>>> list(
            @RequestParam Long contentId) {
        List<CommentResponseDto> list = commentService.listCommentsByContent(contentId);
        return ResponseEntity.ok(
                ApiResponseDto.<List<CommentResponseDto>>builder()
                        .message("Fetched comments")
                        .statusCode(HttpStatus.OK.value())
                        .data(list)
                        .build());
    }

    /** 댓글 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CommentResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateDto req) {
        CommentResponseDto dto = commentService.updateComment(id, req);
        return ResponseEntity.ok(
                ApiResponseDto.<CommentResponseDto>builder()
                        .message("Comment updated")
                        .statusCode(HttpStatus.OK.value())
                        .data(dto)
                        .build());
    }

    /** 댓글 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @PathVariable Long id,
            @RequestParam String password) {
        commentService.deleteComment(id, password);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("Comment deleted")
                        .statusCode(HttpStatus.OK.value())
                        .data(null)
                        .build());
    }
}
