package com.example.devnote.processor_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 게시글에 달린 비회원 댓글
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대댓글이 아니라면 parentId = null */
    private Long parentId;

    /** 댓글이 달린 콘텐츠(ID) */
    @Column(nullable = false)
    private Long contentId;

    /** 작성자 이름(임의 입력) */
    @Column(nullable = false, length = 50)
    private String username;

    /** 비회원용 비밀번호 해시 */
    @Column(nullable = false)
    private String passwordHash;

    /** 댓글 내용 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 생성 시각 자동 저장 */
    @CreationTimestamp
    private Instant createdAt;

    /** 수정 시각 자동 갱신 */
    @UpdateTimestamp
    private Instant updatedAt;
}
