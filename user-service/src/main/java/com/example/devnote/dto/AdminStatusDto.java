package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관리자 여부 확인 API의 응답 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatusDto {
    private boolean isAdmin;
}
