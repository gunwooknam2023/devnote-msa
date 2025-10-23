package com.example.devnote.dto;

import com.example.devnote.entity.enums.CommentTargetType;
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
    private CommentTargetType targetType;
    private Long targetId;
    private Long userId;
    private String username;
    private String userPicture;
    private String content;
    private String targetTitle;
    private String targetSource;
    private String targetLink;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CommentResponseDto> replies;
    private int likeCount;
    private int dislikeCount;
}