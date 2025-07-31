package com.example.devnote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequestDto {
    @NotNull(message = "contentId is required")
    private Long contentId;

    private Long parentId;

    @NotBlank(message = "content is required")
    private String content;

    /** 비회원 댓글용 */
    private String username;
    private String password;
}