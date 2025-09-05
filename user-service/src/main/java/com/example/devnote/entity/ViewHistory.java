package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 사용자의 콘텐츠 시청 기록을 저장하는 엔티티
 * 한 사용자는 하나의 콘텐츠에 대해 하나의 시청 기록만 가집니다. (재시청 시 시간만 업데이트)
 */
@Entity
@Table(name = "view_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "content_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 시청한 콘텐츠의 ID
     */
    @Column(name = "content_id", nullable = false)
    private Long contentId;

    /**
     * 마지막으로 시청한 시각
     */
    @UpdateTimestamp
    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;
}