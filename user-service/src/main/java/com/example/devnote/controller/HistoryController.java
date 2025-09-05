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
}
