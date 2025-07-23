package com.example.devnote.processor_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 댓글 수정 DTO
 */
@Data
public class CommentUpdateDto {
    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "content is required")
    private String content;
}
