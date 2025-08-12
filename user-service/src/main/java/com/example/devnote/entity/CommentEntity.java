package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_content_parent_created", columnList = "contentId,parentId,createdAt"),
        @Index(name = "idx_comments_user", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대댓글일 땐 parentId, 아니면 null */
    private Long parentId;

    /** processor-service 의 content.id */
    @Column(nullable = false)
    private Long contentId;

    /** 회원 댓글일 때 채워지는 회원 ID (anonymous 면 null) */
    private Long userId;

    /** 댓글 작성자 표시명 (회원이면 DB의 name, 비회원이면 입력된 username) */
    @Column(nullable = false, length = 50)
    private String username;

    /** 비회원 댓글일 때만 채워짐 (회원 댓글은 null) */
    @Column
    private String passwordHash;

    /** 댓글 본문 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 소프트 삭제 여부
     * - true: 내용만 삭제, 나머지 정보는 유지
     * - false: 일반 댓글
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}