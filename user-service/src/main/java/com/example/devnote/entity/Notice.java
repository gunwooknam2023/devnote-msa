package com.example.devnote.entity;

import com.example.devnote.entity.enums.NoticeCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 공지사항 정보를 담는 엔티티
 */
@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 공지사항을 작성한 관리자(User)의 ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 공지사항 카테고리 (Enum 타입) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeCategory category;

    /** 공지사항 제목 */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 공지사항 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}