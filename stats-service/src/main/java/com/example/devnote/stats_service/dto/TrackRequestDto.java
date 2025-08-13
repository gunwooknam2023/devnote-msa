package com.example.devnote.stats_service.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TrackRequestDto {
    /** 방문자 ID */
    @Size(max = 128)
    private String visitorId;
}