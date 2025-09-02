package com.example.devnote.processor_service.controller;

import com.example.devnote.processor_service.dto.ApiResponseDto;
import com.example.devnote.processor_service.es.EsContent;
import com.example.devnote.processor_service.service.ContentSearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contents/search")
public class ContentSearchController {
    private final ContentSearchService contentSearchService;

    /**
     * 콘텐츠 검색 API
     * @param q 검색할 키워드
     * @param source 검색 대상 소스 ("YOUTUBE" 또는 "NEWS")
     * @param category (선택) 필터링할 카테고리
     * @param sort (선택) 정렬 옵션
     * @param pageable 페이지네이션 정보
     * @param request 요청 객체
     */
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<EsContent>>> search(
            @RequestParam("q") String q,
            @RequestParam("source") String source,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "relevance") String sort,
            @PageableDefault(size = 24) Pageable pageable,
            HttpServletRequest request) {

        Page<EsContent> result = contentSearchService.search(q, source, category, sort, pageable, request);

        return ResponseEntity.ok(
                ApiResponseDto.<Page<EsContent>>builder()
                        .message("Content search successful")
                        .statusCode(200)
                        .data(result)
                        .build()
        );
    }
}
