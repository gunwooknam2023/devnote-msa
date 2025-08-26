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

    /**
     * 문의에 첨부된 이미지 목록
     * CascadeType.ALL: Inquiry 엔티티의 생명주기에 InquiryImage를 맞춤 (저장/삭제 시 함께 처리)
     * orphanRemoval = true: Inquiry의 images 컬렉션에서 InquiryImage가 제거되면 DB에서도 삭제
     */
    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InquiryImage> images = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void addImage(InquiryImage image) {
        images.add(image);
        image.setInquiry(this);
    }
}