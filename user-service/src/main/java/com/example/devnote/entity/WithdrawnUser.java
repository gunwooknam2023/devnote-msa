package com.example.devnote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 탈퇴한 사용자의 재가입 방지를 위한 정보를 저장하는 엔티티
 * 개인정보 최소화 원칙에 따라, 재가입 방지에 필요한 최소 정보만 저장
 */
@Entity
@Table(name = "withdrawn_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawnUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false, updatable = false)
    private Instant withdrawnAt; // 탈퇴한 시각

    @Column(nullable = false)
    private Instant canRejoinAt; // 재가입이 가능해지는 시각 (탈퇴 시각 + 7일)

    @PrePersist
    protected void onCreate() {
        withdrawnAt = Instant.now();
    }
}