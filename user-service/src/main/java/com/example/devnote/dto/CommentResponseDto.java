package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private Long parentId;
    private Long contentId;
    private String username;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CommentResponseDto> replies;
}