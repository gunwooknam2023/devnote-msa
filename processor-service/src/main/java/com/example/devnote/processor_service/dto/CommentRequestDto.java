package com.example.devnote.processor_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 댓글 요청 dto
 */
@Data
public class CommentRequestDto {
    @NotNull(message = "contentId is required")
    private Long contentId;

    /** 대댓글이라면 parentId 지정 */
    private Long parentId;

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "content is required")
    private String content;
}
