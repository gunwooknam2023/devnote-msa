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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    /** 댓글 생성 */
    @PostMapping
    public ResponseEntity<ApiResponseDto<CommentResponseDto>> create(
            @Valid @RequestBody CommentRequestDto req
    ) {
        CommentResponseDto dto = commentService.createComment(req);
        ApiResponseDto<CommentResponseDto> body = ApiResponseDto
                .<CommentResponseDto>builder()
                .message("Comment created")
                .statusCode(HttpStatus.CREATED.value())
                .data(dto)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** 특정 콘텐츠 댓글 전체 조회 */
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<CommentResponseDto>>> list(
            @RequestParam Long contentId
    ) {
        List<CommentResponseDto> list = commentService.listCommentsByContent(contentId);
        ApiResponseDto<List<CommentResponseDto>> body = ApiResponseDto
                .<List<CommentResponseDto>>builder()
                .message("Fetched comments")
                .statusCode(HttpStatus.OK.value())
                .data(list)
                .build();
        return ResponseEntity.ok(body);
    }

    /** 댓글 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CommentResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateDto req
    ) {
        CommentResponseDto dto = commentService.updateComment(id, req);
        ApiResponseDto<CommentResponseDto> body = ApiResponseDto
                .<CommentResponseDto>builder()
                .message("Comment updated")
                .statusCode(HttpStatus.OK.value())
                .data(dto)
                .build();
        return ResponseEntity.ok(body);
    }

    /** 댓글 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @PathVariable Long id,
            @RequestParam(required = false) String password
    ) {
        commentService.deleteComment(id, password);
        ApiResponseDto<Void> body = ApiResponseDto
                .<Void>builder()
                .message("Comment deleted")
                .statusCode(HttpStatus.OK.value())
                .data(null)
                .build();
        return ResponseEntity.ok(body);
    }

    /** 여러 콘텐츠의 댓글 수를 일괄 조회 */
    @GetMapping("/counts")
    public ResponseEntity<ApiResponseDto<Map<Long, Integer>>> getCommentCounts(
            @RequestParam List<Long> contentIds
    ) {
        Map<Long, Integer> counts = commentService.getCommentCounts(contentIds);
        return ResponseEntity.ok(
                ApiResponseDto.<Map<Long, Integer>>builder()
                        .message("Fetched comment counts")
                        .statusCode(HttpStatus.OK.value())
                        .data(counts)
                        .build()
        );
    }
}
