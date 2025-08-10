package com.example.devnote.processor_service.controller;

import com.example.devnote.processor_service.dto.ContentDto;
import com.example.devnote.processor_service.service.ContentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContentRedirectController {
    private final ContentService contentService;

    @GetMapping("/r/{id}")
    public ResponseEntity<Void> redirect(@PathVariable Long id, HttpServletRequest req) {
        // 1) 로컬 조회수 증가
        contentService.countView(id, req);

        // 2) 원본 링크로 302 리다이렉트
        ContentDto dto = contentService.getContentById(id);
        return ResponseEntity.status(302)
                .header("Location", dto.getLink())
                .build();
    }
}
