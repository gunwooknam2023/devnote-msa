package com.example.devnote.stats_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HeartbeatRequestDto {
    @NotBlank
    private String pageViewId;
}
