package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 문의사항에 첨부된 이미지 URL을 저장하는 엔티티
 */
@Entity
@Table(name = "inquiry_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이미지 URL */
    @Column(nullable = false, length = 512)
    private String imageUrl;

    /** 이 이미지가 속한 문의사항 (N:1 관계) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;
}