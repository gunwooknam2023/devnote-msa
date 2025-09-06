package com.example.devnote.controller;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.ContentDto;
import com.example.devnote.dto.ViewHistoryRequestDto;
import com.example.devnote.service.HistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 시청 기록을 저장(또는 업데이트)
     */
    @PostMapping("/view")
    public ResponseEntity<ApiResponseDto<Void>> recordView(@Valid @RequestBody ViewHistoryRequestDto requestDto) {
        historyService.recordViewHistory(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.<Void>builder()
                        .message("시청 기록이 저장되었습니다.")
                        .statusCode(HttpStatus.CREATED.value())
                        .build());
    }

    /**
     * 현재 로그인된 사용자의 시청 기록 목록을 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<ContentDto>>> getHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ContentDto> historyPage = historyService.getViewHistory(pageable);
        return ResponseEntity.ok(
                ApiResponseDto.<Page<ContentDto>>builder()
                        .message("시청 기록을 조회했습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(historyPage)
                        .build()
        );
    }

    /**
     * 특정 콘텐츠에 대한 시청 기록을 개별 삭제
     * @param contentId 삭제할 콘텐츠의 ID
     */
    @DeleteMapping("/{contentId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteHistory(@PathVariable Long contentId) {
        historyService.deleteHistoryByContentId(contentId);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("선택한 시청 기록이 삭제되었습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .build()
        );
    }

    /**
     * 특정 소스(YOUTUBE/NEWS)에 대한 모든 시청 기록을 삭제
     * @param source "YOUTUBE" 또는 "NEWS"
     */
    @DeleteMapping
    public ResponseEntity<ApiResponseDto<Void>> deleteAllHistory(@RequestParam String source) {
        // source 파라미터가 유효한지 확인
        if (!"YOUTUBE".equalsIgnoreCase(source) && !"NEWS".equalsIgnoreCase(source)) {
            return ResponseEntity.badRequest().body(
                    ApiResponseDto.<Void>builder()
                            .message("source 파라미터는 YOUTUBE 또는 NEWS만 가능합니다.")
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .build()
            );
        }

        historyService.deleteAllHistory(source.toUpperCase());

        String message = String.format("%s 시청 기록이 모두 삭제되었습니다.", source.toUpperCase());
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message(message)
                        .statusCode(HttpStatus.OK.value())
                        .build()
        );
    }
}
