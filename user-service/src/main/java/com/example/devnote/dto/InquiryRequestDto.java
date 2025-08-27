package com.example.devnote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 문의사항 생성 API 요청 DTO
 */
@Data
public class InquiryRequestDto {

    @NotBlank(message = "제목은 필수 항목입니다.")
    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "내용은 필수 항목입니다.")
    private String content;

    /** 비회원 문의용 사용자명 */
    @Size(max = 20, message = "사용자명은 최대 20자까지 가능합니다.")
    private String username;

    /** 비회원 문의용 비밀번호 */
    @Size(max = 20, message = "비밀번호는 최대 20자까지 가능합니다.")
    private String password;

    /** 공개글 여부 (기본값: 공개) */
    private Boolean isPublic = true;
}