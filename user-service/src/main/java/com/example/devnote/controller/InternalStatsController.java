package com.example.devnote.controller;

import com.example.devnote.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 서비스 간 통신(내부용)을 위한 통계 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stats")
public class InternalStatsController {

    private final CommentService commentService;

    @GetMapping("/comment/count-by-day")
    public ResponseEntity<Map<String, Long>> getCommentCountByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        long count = commentService.countByDay(date);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
