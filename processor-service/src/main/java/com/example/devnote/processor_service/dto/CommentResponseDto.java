package com.example.devnote.processor_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 댓글 반환 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private Long id;
    private Long parentId; // null 이면 루트 댓글
    private Long contentId;
    private String username;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    /** 답글이 있을 경우 포함 */
    private List<CommentResponseDto> replies;
}
