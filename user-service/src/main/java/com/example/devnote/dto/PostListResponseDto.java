package com.example.devnote.dto;

import com.example.devnote.entity.enums.BoardType;
import com.example.devnote.entity.enums.StudyCategory;
import com.example.devnote.entity.enums.StudyMethod;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 게시글 목록 조회 응답 DTO
 */
@Data
@Builder
public class PostListResponseDto {

    private Long id;
    private BoardType boardType;
    private String title;

    // 작성자 정보
    private String authorName;
    private String authorPicture;

    // 통계 정보
    private long viewCount;
    private long commentCount;
    private long likeCount;
    private long dislikeCount;
    private long scrapCount;

    // Q&A 전용
    private boolean isAdopted;

    // 스터디 전용
    private StudyCategory studyCategory;
    private StudyMethod studyMethod;
    private boolean isRecruiting;

    private Instant createdAt;
}
