package com.example.devnote.stats_service.controller;

import com.example.devnote.stats_service.dto.*;
import com.example.devnote.stats_service.service.DurationStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 페이지 체류 시간 통계 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stats/duration")
public class DurationStatsController {

    private final DurationStatsService service;

    /**
     * 프론트엔드로부터 주기적인 하트비트 신호를 받아 처리합니다.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@Valid @RequestBody HeartbeatRequestDto req) {
        service.handleHeartbeat(req.getPageViewId());
        return ResponseEntity.ok().build();
    }

    /** 기간별 일일 체류시간 조회 */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponseDto<List<DailyCountDto>>> getDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<DailyCountDto> data = service.getDailyStats(start, end);
        return ResponseEntity.ok(ApiResponseDto.<List<DailyCountDto>>builder()
                .message("Daily page view duration stats (seconds)")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 특정 연도의 월별 체류시간 조회 */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponseDto<List<MonthlyCountDto>>> getMonthly(@RequestParam int year) {
        List<MonthlyCountDto> data = service.getMonthlyStats(year);
        return ResponseEntity.ok(ApiResponseDto.<List<MonthlyCountDto>>builder()
                .message("Monthly page view duration stats for " + year + " (seconds)")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 기간별 연간 체류시간 조회 */
    @GetMapping("/yearly")
    public ResponseEntity<ApiResponseDto<List<YearlyCountDto>>> getYearly(
            @RequestParam int startYear, @RequestParam int endYear) {
        List<YearlyCountDto> data = service.getYearlyStats(startYear, endYear);
        return ResponseEntity.ok(ApiResponseDto.<List<YearlyCountDto>>builder()
                .message("Yearly page view duration stats (seconds)")
                .statusCode(200)
                .data(data)
                .build());
    }
}