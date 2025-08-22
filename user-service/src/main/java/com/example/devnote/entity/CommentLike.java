package com.example.devnote.entity;

import com.example.devnote.entity.enums.VoteType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자의 댓글 좋아요/싫어요 투표 정보를 저장하는 엔티티
 * 한 사용자는 하나의 댓글에 한 번만 투표할 수 있도록 uniqueConstraints를 설정
 */
@Entity
@Table(name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "comment_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommentEntity comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType voteType;
}
