package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 찜한 아이템 (유튜브 영상, 뉴스, 채널)
 */
@Entity
@Table(name = "favorites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String type;    // YOUTUBE, CHANNEL, NEWS

    @Column(nullable = false)
    private String itemId;  // YOUTUBE 영상 ID, 채널 ID, 뉴스 KEY
}
