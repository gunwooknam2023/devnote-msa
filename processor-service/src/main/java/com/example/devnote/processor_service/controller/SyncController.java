package com.example.devnote.processor_service.controller;

import com.example.devnote.processor_service.dto.ApiResponseDto;
import com.example.devnote.processor_service.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * MariaDB와 Elasticsearch 간의 데이터 동기화를 위한 관리자용 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/sync")
public class SyncController {
    private final SyncService syncService;

    /**
     * 지정된 기간의 콘텐츠 데이터를 MariaDB에서 Elasticsearch로 동기화
     * @param start 동기화 시작일 (YYYY-MM-DD)
     * @param end   동기화 종료일 (YYYY-MM-DD)
     */
    @PostMapping("/contents")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> syncContents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        long syncedCount = syncService.syncContentsByDateRange(start, end);

        Map<String, Object> responseData = Map.of(
                "period", start + " to " + end,
                "syncedCount", syncedCount
        );

        return ResponseEntity.ok(
                ApiResponseDto.<Map<String, Object>>builder()
                        .message("Data synchronization triggered successfully.")
                        .statusCode(200)
                        .data(responseData)
                        .build()
        );
    }
}
