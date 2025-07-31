package com.example.devnote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentUpdateDto {
    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "content is required")
    private String content;
}
