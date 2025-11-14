//package com.example.devnote.stats_service.controller;
//
//import com.example.devnote.stats_service.dto.ApiResponseDto;
//import com.example.devnote.stats_service.dto.DailyCountDto;
//import com.example.devnote.stats_service.dto.MonthlyCountDto;
//import com.example.devnote.stats_service.dto.YearlyCountDto;
//import com.example.devnote.stats_service.service.ContentViewStatsService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.util.List;
//
///**
// * 콘텐츠 조회수 통계 API 컨트롤러
// */
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/v1/stats/content-view")
//public class ContentViewStatsController {
//
//    private final ContentViewStatsService service;
//
//    /** 기간별 일일 콘텐츠 조회수 조회 */
//    @GetMapping("/daily")
//    public ResponseEntity<ApiResponseDto<List<DailyCountDto>>> getDaily(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
//        List<DailyCountDto> data = service.getDailyStats(start, end);
//        return ResponseEntity.ok(ApiResponseDto.<List<DailyCountDto>>builder()
//                .message("Daily content view stats")
//                .statusCode(200)
//                .data(data)
//                .build());
//    }
//
//    /** 특정 연도의 월별 콘텐츠 조회수 조회 */
//    @GetMapping("/monthly")
//    public ResponseEntity<ApiResponseDto<List<MonthlyCountDto>>> getMonthly(@RequestParam int year) {
//        List<MonthlyCountDto> data = service.getMonthlyStats(year);
//        return ResponseEntity.ok(ApiResponseDto.<List<MonthlyCountDto>>builder()
//                .message("Monthly content view stats for " + year)
//                .statusCode(200)
//                .data(data)
//                .build());
//    }
//
//    /** 기간별 연간 콘텐츠 조회수 조회 */
//    @GetMapping("/yearly")
//    public ResponseEntity<ApiResponseDto<List<YearlyCountDto>>> getYearly(
//            @RequestParam int startYear, @RequestParam int endYear) {
//        List<YearlyCountDto> data = service.getYearlyStats(startYear, endYear);
//        return ResponseEntity.ok(ApiResponseDto.<List<YearlyCountDto>>builder()
//                .message("Yearly content view stats")
//                .statusCode(200)
//                .data(data)
//                .build());
//    }
//
//    /**
//     * 지정된 기간의 콘텐츠 조회수 통계를 재처리
//     * ** 추후 권한 부여 필요 (관리자용)
//     */
//    @PostMapping("/backfill")
//    public ResponseEntity<ApiResponseDto<String>> backfill(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
//        service.backfillHistoricalStats(start, end);
//        String message = String.format("Backfill for content view stats from %s to %s has been triggered.", start, end);
//        return ResponseEntity.ok(ApiResponseDto.<String>builder()
//                .message(message)
//                .statusCode(200)
//                .data(null)
//                .build());
//    }
//}
