package com.example.devnote.processor_service.controller;

import com.example.devnote.processor_service.dto.ApiResponseDto;
import com.example.devnote.processor_service.dto.ContentDto;
import com.example.devnote.processor_service.dto.PageResponseDto;
import com.example.devnote.processor_service.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 콘텐츠 조회용 REST API
 *  - /api/v1/contents        : 페이징된 콘텐츠 리스트 조회
 *  - /api/v1/contents/{id}   : 단일 콘텐츠 상세 조회
 *  - /api/v1/contents/search : 키워드 검색
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contents")
@Slf4j
public class ContentController {
    private final ContentService contentService;

    /** 페이징된 콘텐츠 리스트 조회 → DB 조회 후 PageResponseDto 반환 */
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<ContentDto>>> list(
            @RequestParam String source,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit
    ) {
        log.info("API list() source={}, category={}, cursor={}, limit={}",
                source, category, cursor, limit);

        PageResponseDto<ContentDto> page =
                contentService.getContents(source, category, cursor, limit);

        return ResponseEntity.ok(ApiResponseDto.<PageResponseDto<ContentDto>>builder()
                .message("Fetched content list")
                .statusCode(200)
                .data(page)
                .build());
    }

    /** 헬스 체크: 서비스 정상 여부 반환 */
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<String>> health() {
        log.info("API /health called");
        return ResponseEntity.ok(
                ApiResponseDto.<String>builder()
                        .message("OK")
                        .statusCode(200)
                        .data("processor-service is up")
                        .build()
        );
    }
}
