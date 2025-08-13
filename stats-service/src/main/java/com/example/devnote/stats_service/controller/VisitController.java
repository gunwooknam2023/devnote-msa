package com.example.devnote.stats_service.controller;

import com.example.devnote.stats_service.dto.*;
import com.example.devnote.stats_service.service.VisitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 방문자 통계 컨트롤러
 * - 12시간 버킷(00~11, 12~23)당 1회만 카운트 → 하루 최대 2회
 * - 오늘/일별/시간별 조회 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visits")
public class VisitController {

    private final VisitService service;

    /** 방문 트래킹 (하루 최대 2회: 12시간 버킷당 1회) */
    @PostMapping("/track")
    public ResponseEntity<ApiResponseDto<TrackResponseDto>> track(
            @Valid @RequestBody TrackRequestDto req,
            HttpServletRequest http
    ) {
        TrackResponseDto data = service.track(req, http);
        return ResponseEntity.ok(ApiResponseDto.<TrackResponseDto>builder()
                .message("Tracked")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 오늘의 방문자 수 (로그인/비로그인 합산) */
    @GetMapping("/today")
    public ResponseEntity<ApiResponseDto<TodayCountDto>> today() {
        TodayCountDto data = service.getTodayCount();
        return ResponseEntity.ok(ApiResponseDto.<TodayCountDto>builder()
                .message("Today's visitors")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 기간 일별 방문자 수 (그래프용) */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponseDto<List<DailyCountDto>>> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<DailyCountDto> data = service.getDailyCounts(start, end);
        return ResponseEntity.ok(ApiResponseDto.<List<DailyCountDto>>builder()
                .message("Daily visitors")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 특정 날짜 시간대별 방문자 수 (그래프용) */
    @GetMapping("/hourly")
    public ResponseEntity<ApiResponseDto<List<HourlyCountDto>>> hourly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<HourlyCountDto> data = service.getHourlyCounts(date);
        return ResponseEntity.ok(ApiResponseDto.<List<HourlyCountDto>>builder()
                .message("Hourly visitors")
                .statusCode(200)
                .data(data)
                .build());
    }
}