package com.example.devnote.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ViewHistoryRequestDto {
    @NotNull(message = "콘텐츠 ID는 필수입니다.")
    private Long contentId;
}