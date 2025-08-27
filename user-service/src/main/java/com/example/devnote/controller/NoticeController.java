package com.example.devnote.controller;

import com.example.devnote.dto.*;
import com.example.devnote.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 이미지 업로드 (관리자 전용)
     */
    @PostMapping("/images")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> uploadImage(@RequestParam("image") MultipartFile imageFile) {
        String imageUrl = noticeService.uploadImage(imageFile);
        return ResponseEntity.ok(
                ApiResponseDto.<Map<String, String>>builder()
                        .message("이미지가 성공적으로 업로드되었습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(Map.of("imageUrl", imageUrl))
                        .build());
    }

    /**
     * 공지사항 생성 (관리자 전용)
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto<NoticeDetailResponseDto>> createNotice(@Valid @RequestBody NoticeRequestDto requestDto) {
        NoticeDetailResponseDto responseDto = noticeService.createNotice(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.<NoticeDetailResponseDto>builder()
                        .message("공지사항이 등록되었습니다.")
                        .statusCode(HttpStatus.CREATED.value())
                        .data(responseDto)
                        .build());
    }

    /**
     * 공지사항 목록 조회 (누구나 가능)
     */
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<NoticeListResponseDto>>> getNoticeList(@PageableDefault(size = 10) Pageable pageable) {
        Page<NoticeListResponseDto> responsePage = noticeService.getNoticeList(pageable);
        return ResponseEntity.ok(
                ApiResponseDto.<Page<NoticeListResponseDto>>builder()
                        .message("공지사항 목록을 조회했습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(responsePage)
                        .build());
    }

    /**
     * 공지사항 상세 조회 (누구나 가능)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<NoticeDetailResponseDto>> getNoticeDetail(@PathVariable Long id) {
        NoticeDetailResponseDto responseDto = noticeService.getNoticeDetail(id);
        return ResponseEntity.ok(
                ApiResponseDto.<NoticeDetailResponseDto>builder()
                        .message("공지사항 상세 정보를 조회했습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(responseDto)
                        .build());
    }

    /**
     * 공지사항 수정 (관리자 전용)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<NoticeDetailResponseDto>> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequestDto requestDto) {
        NoticeDetailResponseDto responseDto = noticeService.updateNotice(id, requestDto);
        return ResponseEntity.ok(
                ApiResponseDto.<NoticeDetailResponseDto>builder()
                        .message("공지사항이 수정되었습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(responseDto)
                        .build());
    }

    /**
     * 공지사항 삭제 (관리자 전용)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("공지사항이 삭제되었습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(null)
                        .build());
    }

    /**
     * 현재 로그인한 사용자가 관리자인지 여부를 반환하는 API
     */
    @GetMapping("/me/is-admin")
    public ResponseEntity<ApiResponseDto<AdminStatusDto>> isAdmin() {
        AdminStatusDto adminStatus = noticeService.checkAdminStatus();
        return ResponseEntity.ok(
                ApiResponseDto.<AdminStatusDto>builder()
                        .message("Admin status checked successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .data(adminStatus)
                        .build()
        );
    }
}