package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

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

    /** 대댓글일 땐 parentId, 아니면 null */
    private Long parentId;

    /** processor-service 의 content.id */
    @Column(nullable = false)
    private Long contentId;

    /** 작성자 이름 */
    @Column(nullable = false, length = 50)
    private String username;

    /** 비밀번호 해시(BCrypt) */
    @Column(nullable = false)
    private String passwordHash;

    /** 댓글 본문 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}