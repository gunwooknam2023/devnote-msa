package com.example.devnote.entity;

import com.example.devnote.entity.enums.ReportReason;
import com.example.devnote.entity.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 신고 내역을 저장하는 엔티티
 */
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_target", columnList = "targetType, targetId"),
        @Index(name = "idx_report_author", columnList = "authorId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType; // 신고 대상 타입 (CONTENT, COMMENT, POST)

    @Column(nullable = false)
    private Long targetId; // 신고된 콘텐츠 또는 댓글의 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason; // 신고 사유

    @Column(columnDefinition = "TEXT")
    private String targetContentSnapshot; // 신고 시점의 콘텐츠 제목 또는 댓글 내용 (증거 보존용)

    @Column(length = 100)
    private String authorName; // 신고 대상 작성자 닉네임

    @Column
    private Long authorId; // 신고 대상 작성자 ID (회원인 경우)

    @Column(length = 255)
    private String targetTitle; // 신고 대상 제목 (게시글인 경우)

    @Column(columnDefinition = "TEXT")
    private String targetContent; // 신고 대상 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter; // 신고한 회원 (비회원인 경우 null)

    @Column(nullable = false)
    private String reporterIp; // 신고자 IP 주소 (중복 방지용)

    @Column(columnDefinition = "TEXT")
    private String reasonDetail; // '기타' 사유 선택 시 상세 내용을 저장

    @Builder.Default
    private boolean isProcessed = false; // 관리자 처리 완료 여부

    @CreationTimestamp
    private Instant createdAt; // 신고 시간
}