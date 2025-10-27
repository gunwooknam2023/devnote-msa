package com.example.devnote.entity;

import com.example.devnote.entity.enums.BoardType;
import com.example.devnote.entity.enums.StudyCategory;
import com.example.devnote.entity.enums.StudyMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 게시글 엔티티
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_board_type_created_at", columnList = "boardType, createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 게시글 작성 유저
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 게시판 종류 (FREE_BOARD, QNA, STUDY)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardType boardType;

    /**
     * 게시글 제목
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 게시글 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 조회수
     */
    @Column(nullable = false)
    @Builder.Default
    private long viewCount = 0L;

    //////////////// Q&A 게시판 전용 /////////////////

    /**
     * 답변 채택 여부
     */
    private boolean isAdopted;

    /**
     * 채택된 댓글의 ID
     */
    private Long adoptedCommentId;

    //////////////// 스터디 게시판 전용 /////////////////

    /**
     * 스터디 카테고리
     */
    @Enumerated(EnumType.STRING)
    private StudyCategory studyCategory;

    /**
     * 스터디 진행 방식 (ONLINE, OFFLINE, HYBRID)
     */
    @Enumerated(EnumType.STRING)
    private StudyMethod studyMethod;

    /**
     * 스터디 모집 상태 (true: 모집중, false: 모집완료)
     */
    @Builder.Default
    private boolean isRecruiting = true;

    /**
     * 좋아요 수
     */
    @Column(nullable = false)
    @Builder.Default
    private long likeCount = 0L;

    /**
     * 싫어요 수
     */
    @Column(nullable = false)
    @Builder.Default
    private long dislikeCount = 0L;

    /**
     * 스크랩 수
     */
    @Column(nullable = false)
    @Builder.Default
    private long scrapCount = 0L;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
