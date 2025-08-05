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
    private Long userId;
    private String username;
    private String userPicture;
    private String content;
    private String contentTitle;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CommentResponseDto> replies;
}