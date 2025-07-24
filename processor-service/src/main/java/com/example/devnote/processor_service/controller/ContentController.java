package com.example.devnote.processor_service.controller;

import com.example.devnote.processor_service.dto.ApiResponseDto;
import com.example.devnote.processor_service.dto.ContentDto;
import com.example.devnote.processor_service.dto.PageResponseDto;
import com.example.devnote.processor_service.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
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

    /**
     * 페이지네이션, 필터, 정렬 적용된 콘텐츠 리스트 조회
     *
     * @param page     요청 페이지 (기본 0)
     * @param size     페이지당 항목 수 (기본 20)
     * @param source   소스 필터 ("NEWS","YOUTUBE"), 미지정 시 전체
     * @param category 카테고리 필터, 미지정 시 전체
     * @param title    제목 키워드 포함 검색, 미지정 시 전체
     * @param sort     정렬 순서 ("newest" or "oldest"), 기본 newest
     */
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<ContentDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        log.info("API list() page={}, size={}, source={}, category={}, title={}, sort={}",
                page, size, source, category, title, sort);

        PageResponseDto<ContentDto> result = contentService.getContents(page, size, source, category, title, sort);

        return ResponseEntity.ok(
                ApiResponseDto.<PageResponseDto<ContentDto>>builder()
                        .message("Fetched content list")
                        .statusCode(200)
                        .data(result)
                        .build()
        );
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
