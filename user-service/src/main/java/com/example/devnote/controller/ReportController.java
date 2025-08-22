package com.example.devnote.controller;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.ReportRequestDto;
import com.example.devnote.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고 접수
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto<Void>> createReport(
            @Valid @RequestBody ReportRequestDto reportRequestDto,
            HttpServletRequest request
    ) {
        reportService.createReport(reportRequestDto, request);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("신고가 정상적으로 접수되었습니다.")
                        .statusCode(200)
                        .build()
        );
    }

    /**
     * 신고 처리 상태 변경 (관리자용)
     */
    @PatchMapping("/{id}/process")
    public ResponseEntity<ApiResponseDto<Void>> processReport(
            @PathVariable Long id,
            @RequestParam boolean processed
    ) {
        // TODO: 관리자 권한을 확인하는 로직 추가 필요
        reportService.processReport(id, processed);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("신고 처리 상태가 변경되었습니다.")
                        .statusCode(200)
                        .build()
        );
    }
}