package com.example.devnote.dto;

import com.example.devnote.entity.WithdrawnUser;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 탈퇴한 사용자의 정보를 프론트엔드에 전달하기 위한 DTO
 */
@Data
@Builder
public class WithdrawnUserDto {
    private Instant withdrawnAt;
    private Instant rejoinAvailableAt;

    /**
     * WithdrawnUser 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static WithdrawnUserDto from(WithdrawnUser entity) {
        return WithdrawnUserDto.builder()
                .withdrawnAt(entity.getWithdrawnAt())
                .rejoinAvailableAt(entity.getCanRejoinAt())
                .build();
    }
}
