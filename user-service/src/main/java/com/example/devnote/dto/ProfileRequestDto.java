package com.example.devnote.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileRequestDto {
    @Size(max = 500, message = "최대 500자까지 입력 가능합니다.")
    private String selfIntroduction;
}
