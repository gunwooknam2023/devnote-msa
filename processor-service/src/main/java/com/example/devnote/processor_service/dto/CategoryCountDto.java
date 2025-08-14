package com.example.devnote.processor_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카테고리별 콘텐츠 개수 조회 결과를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCountDto {
    private String category;
    private long count;
}