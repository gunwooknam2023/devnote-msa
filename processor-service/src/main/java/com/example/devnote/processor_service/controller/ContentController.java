package com.example.devnote.processor_service.controller;

import com.example.devnote.processor_service.dto.ApiResponseDto;
import com.example.devnote.processor_service.dto.ContentDto;
import com.example.devnote.processor_service.dto.PageResponseDto;
import com.example.devnote.processor_service.service.ContentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
            @RequestParam(required = false) String channelId,
            @RequestParam(required = false) String channelTitle,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        log.info("API list() page={}, size={}, source={}, category={}, title={}, sort={}",
                page, size, source, category, title, sort);

        PageResponseDto<ContentDto> result = contentService.getContents(page, size, source, category, channelId, channelTitle, title, sort);

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


    /** 단일 콘텐츠 조회 (찜 기능용 존재 확인) */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ContentDto>> getById(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
         ContentDto dto = contentService.getContentById(id);

        return ResponseEntity.ok(
                ApiResponseDto.<ContentDto>builder()
                        .message("Fetched content")
                        .statusCode(HttpStatus.OK.value())
                        .data(dto)
                        .build()
        );
    }

    /**
     * 조회수 증가
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView (@PathVariable Long id, HttpServletRequest req) {
        contentService.countView(id, req);

        return ResponseEntity.ok().build();
    }

    /**
     * 콘텐츠 삭제 + Kafka로 삭제 이벤트 발행
     * 2025.11.13 임시 주석 처리 -> 추후 인증인가 추가예정
     */
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponseDto<Void>> deleteContent(@PathVariable Long id) {
//        // 1) 존재 검증
//        contentService.verifyExists(id);
//
//        // 2) DB에서 삭제
//        contentService.deleteById(id);
//
//        // 3) Kafka로 삭제 이벤트 전송 (key: 콘텐츠 ID)
//        kafkaTemplate.send("content.deleted", String.valueOf(id));
//
//        return ResponseEntity.ok(
//                ApiResponseDto.<Void>builder()
//                        .message("Content deleted and event published")
//                        .statusCode(200)
//                        .data(null)
//                        .build()
//        );
//    }

    /**
     * 카테고리별 콘텐츠 개수 조회
     * @param source (선택) YOUTUBE 또는 NEWS 로 필터링
     */
    @GetMapping("/category-counts")
    public ResponseEntity<ApiResponseDto<Map<String, Long>>> getCategoryCounts(
            @RequestParam(required = false) String source
    ) {
        Map<String, Long> counts = contentService.getCategoryCounts(source);
        return ResponseEntity.ok(
                ApiResponseDto.<Map<String, Long>>builder()
                        .message("Fetched content counts by category")
                        .statusCode(200)
                        .data(counts)
                        .build()
        );
    }

    /**
     * 삭제/비공개 감지된 콘텐츠 숨김 처리
     * - 프론트엔드에서 썸네일 120x90 감지 시 호출
     * - 해당 콘텐츠를 HIDDEN 상태로 변경
     */
    @PostMapping("/{id}/report-unavailable")
    public ResponseEntity<ApiResponseDto<Boolean>> reportUnavailable(@PathVariable Long id) {
        log.info("Received unavailable content report: id={}", id);
        
        boolean hidden = contentService.hideContent(id);
        
        return ResponseEntity.ok(
                ApiResponseDto.<Boolean>builder()
                        .message(hidden ? "Content hidden successfully" : "Content already hidden or not found")
                        .statusCode(200)
                        .data(hidden)
                        .build()
        );
    }

    /**
     * HIDDEN 상태 콘텐츠를 ES에서 동기화 삭제
     * - 배포 후 한 번 호출하여 기존 HIDDEN 데이터 정리
     */
    @PostMapping("/sync-hidden")
    public ResponseEntity<ApiResponseDto<Integer>> syncHiddenContent() {
        log.info("Starting sync of hidden contents to ES");
        
        int count = contentService.syncHiddenContentToEs();
        
        return ResponseEntity.ok(
                ApiResponseDto.<Integer>builder()
                        .message("Synced hidden contents to ES")
                        .statusCode(200)
                        .data(count)
                        .build()
        );
    }
}
