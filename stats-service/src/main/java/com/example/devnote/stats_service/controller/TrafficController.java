package com.example.devnote.stats_service.controller;

import com.example.devnote.stats_service.dto.*;
import com.example.devnote.stats_service.service.TrafficService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 실시간 트래픽(요청 수) 컨트롤러
 * - 중복 제거 없이 모든 요청을 1건으로 카운트
 * - 분 단위 집계(메서드/경로 포함)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/traffic")
public class TrafficController {

    private final TrafficService service;

    /**
     * 요청 1건 트래킹
     * - 게이트웨이 글로벌 필터에서 자동 호출 (m=메서드, p=경로)
     * - 예: POST /api/v1/traffic/track?m=GET&p=/api/v1/contents
     */
    @PostMapping("/track")
    public ResponseEntity<ApiResponseDto<TrafficTrackResponseDto>> track(
            @RequestParam("m") String method,
            @RequestParam("p") String path
    ) {
        TrafficTrackResponseDto data = service.trackOnce(method, path);
        return ResponseEntity.ok(ApiResponseDto.<TrafficTrackResponseDto>builder()
                .message("Tracked")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 오늘 총 요청 수 */
    @GetMapping("/today")
    public ResponseEntity<ApiResponseDto<TodayCountDto>> today() {
        TodayCountDto data = service.getToday();
        return ResponseEntity.ok(ApiResponseDto.<TodayCountDto>builder()
                .message("Today's traffic")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 기간 일별 요청 수 */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponseDto<List<DailyCountDto>>> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<DailyCountDto> data = service.getDaily(start, end);
        return ResponseEntity.ok(ApiResponseDto.<List<DailyCountDto>>builder()
                .message("Daily traffic")
                .statusCode(200)
                .data(data)
                .build());
    }

    /** 특정 날짜 시간별 요청 수 */
    @GetMapping("/hourly")
    public ResponseEntity<ApiResponseDto<List<HourlyCountDto>>> hourly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<HourlyCountDto> data = service.getHourly(date);
        return ResponseEntity.ok(ApiResponseDto.<List<HourlyCountDto>>builder()
                .message("Hourly traffic")
                .statusCode(200)
                .data(data)
                .build());
    }
}
