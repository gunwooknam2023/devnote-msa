package com.example.devnote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentUpdateDto {
    /** 비회원 댓글 수정 시 필요 */
    private String password;

    @NotBlank(message = "content is required")
    @Size(max = 1000, message = "최대 1000자까지 입력 가능합니다.")
    private String content;
}
