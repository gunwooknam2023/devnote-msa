package com.example.devnote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentUpdateDto {
    /** 비회원 댓글 수정 시 필요 */
    private String password;

    @NotBlank(message = "content is required")
    private String content;
}
