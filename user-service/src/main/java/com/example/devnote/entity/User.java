package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider;    // 구글, 네이버, 카카오

    @Column(nullable = false)
    private String providerId;  // 소셜 ID

    @Column(nullable = false, unique = true)
    private String email;       // 이메일

    private String name;
    private String imageUrl;

    @Column(length = 500)
    private String refreshToken;
}
