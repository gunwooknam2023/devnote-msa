package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 문의사항 정보를 담는 엔티티
 */
@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문의를 작성한 회원 ID (비회원인 경우 null) */
    private Long userId;

    /** 문의 작성자 표시명 (회원이면 DB의 name, 비회원이면 입력된 username) */
    @Column(nullable = false, length = 50)
    private String username;

    /** 비회원 문의일 때만 채워짐 (회원 문의는 null). 비밀번호는 해시되어 저장 */
    @Column
    private String passwordHash;

    /** 문의 제목 */
    @Column(nullable = false, length = 255)
    private String title;

    /** 문의 내용 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 관리자 답변 완료 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean answered = false;

    /** 공개글 여부 (true: 공개, false: 비공개) */
    @Column(nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}