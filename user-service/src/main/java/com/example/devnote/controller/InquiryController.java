package com.example.devnote.controller;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.InquiryDetailResponseDto;
import com.example.devnote.dto.InquiryListResponseDto;
import com.example.devnote.dto.InquiryRequestDto;
import com.example.devnote.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의사항 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto<InquiryDetailResponseDto>> createInquiry(
            @Valid @RequestBody InquiryRequestDto requestDto) {
        InquiryDetailResponseDto responseDto = inquiryService.createInquiry(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.<InquiryDetailResponseDto>builder()
                        .message("문의사항이 성공적으로 등록되었습니다.")
                        .statusCode(HttpStatus.CREATED.value())
                        .data(responseDto)
                        .build());
    }

    /**
     * 문의사항 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<InquiryDetailResponseDto>> getInquiryDetail(@PathVariable Long id,
                                                                                     @RequestParam(required = false) String password) {
        InquiryDetailResponseDto responseDto = inquiryService.getInquiryDetail(id, password);
        return ResponseEntity.ok(
                ApiResponseDto.<InquiryDetailResponseDto>builder()
                        .message("문의사항 상세 정보를 조회했습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(responseDto)
                        .build());
    }

    /**
     * 문의사항 목록 조회 (페이지네이션 적용)
     * 예: /api/v1/inquiries?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<InquiryListResponseDto>>> getInquiryList(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<InquiryListResponseDto> responsePage = inquiryService.getInquiryList(pageable);
        return ResponseEntity.ok(
                ApiResponseDto.<Page<InquiryListResponseDto>>builder()
                        .message("문의사항 목록을 조회했습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(responsePage)
                        .build());
    }

    /**
     * 문의사항 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteInquiry(
            @PathVariable Long id,
            @RequestParam(required = false) String password) {
        inquiryService.deleteInquiry(id, password);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("문의사항이 성공적으로 삭제되었습니다.")
                        .statusCode(HttpStatus.OK.value())
                        .data(null)
                        .build());
    }
}