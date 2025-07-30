package com.example.devnote.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO (accessToken만 담김)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponseDto {
    private String tokenType;
    private String accessToken;
    private String refreshToken;

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(
                    new java.util.HashMap<String, Object>() {{
                        put("tokenType", tokenType);
                        put("accessToken", accessToken);
                    }}
            );
        } catch (Exception e) {
            return "{}";
        }
    }
}