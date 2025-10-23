package com.example.devnote.dto;

import com.example.devnote.entity.enums.BoardType;
import com.example.devnote.entity.enums.StudyCategory;
import com.example.devnote.entity.enums.StudyMethod;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 게시글 상세 조회 응답 DTO
 */
@Data
@Builder
public class PostDetailResponseDto {

    private Long id;
    private BoardType boardType;
    private String title;
    private String content;

    // 작성자 정보
    private Long authorId;
    private String authorName;
    private String authorPicture;

    // 통계 정보
    private long viewCount;
    private long likeCount;
    private long dislikeCount;

    // Q&A 전용
    private boolean isAdopted;
    private Long adoptedCommentId;

    // 스터디 전용
    private StudyCategory studyCategory;
    private StudyMethod studyMethod;
    private boolean isRecruiting;

    private Instant createdAt;
    private Instant updatedAt;
}