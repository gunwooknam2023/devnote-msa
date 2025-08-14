package com.example.devnote.processor_service.config;

import com.google.genai.Client;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google GenAI Client 빈 등록 (Google AI SDK - API Key 방식)
 */
@Configuration
public class GenaiConfig {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Bean
    @SneakyThrows
    public Client genaiClient() {
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }
}