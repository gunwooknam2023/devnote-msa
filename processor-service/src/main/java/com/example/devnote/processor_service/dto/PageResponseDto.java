package com.example.devnote.processor_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {
    private List<T> items; // 조회된 목록
    private int page; // 요청한 페이지 번호
    private int size; // 요청한 페이지 크기
    private long totalElements; // 전체 아이템 수
    private int totalPages; // 전체 페이지 수
}