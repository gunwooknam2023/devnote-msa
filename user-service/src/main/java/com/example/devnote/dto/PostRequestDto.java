package com.example.devnote.dto;

import com.example.devnote.entity.enums.BoardType;
import com.example.devnote.entity.enums.StudyCategory;
import com.example.devnote.entity.enums.StudyMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 게시글 생성 요청 DTO
 */
@Data
public class PostRequestDto {

    /**
     * 게시판 종류
     */
    @NotNull(message = "게시판 종류는 필수 항목입니다.")
    private BoardType boardType;

    /**
     * 게시글 제목
     */
    @NotBlank(message = "제목은 필수 항목입니다.")
    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
    private String title;

    /**
     * 게시글 내용
     */
    @NotBlank(message = "내용은 필수 항목입니다.")
    private String content;

    /**
     * 스터디 카테고리
     */
    private StudyCategory studyCategory;

    /**
     * 스터디 진행 방식
     */
    private StudyMethod studyMethod;
}
