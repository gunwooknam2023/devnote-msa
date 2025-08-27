package com.example.devnote.dto;

import com.example.devnote.entity.enums.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeRequestDto {

    @NotNull(message = "카테고리는 필수 항목입니다.")
    private NoticeCategory category;

    @NotBlank(message = "제목은 필수 항목입니다.")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "내용은 필수 항목입니다.")
    private String content;
}