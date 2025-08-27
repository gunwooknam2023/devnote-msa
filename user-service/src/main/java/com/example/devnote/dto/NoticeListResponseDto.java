package com.example.devnote.dto;

import com.example.devnote.entity.enums.NoticeCategory;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class NoticeListResponseDto {
    private Long id;
    private NoticeCategory category;
    private String title;
    private String authorName;
    private Instant createdAt;
}