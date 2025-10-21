package com.example.devnote.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Q&A 답변 채택 요청 DTO
 */
@Data
public class AdoptionRequestDto {
    /**
     * 채택할 댓글의 ID
     */
    @NotNull
    private Long commentId;
}