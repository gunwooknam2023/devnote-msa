package com.example.devnote.processor_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.genai.Client;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

/**
 * Google GenAI Client 빈 등록
 */
@Configuration
public class GenaiConfig {
    @Value("${gemini.credentials-file}")
    private String credFileName;

    @Value("${gemini.project-id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Bean
    @SneakyThrows
    public Client genaiClient() {
        // 1) JSON 로드
        ClassPathResource res = new ClassPathResource(credFileName);
        GoogleCredentials creds;
        try (InputStream in = res.getInputStream()) {
            creds = ServiceAccountCredentials.fromStream(in)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        }

        // 2) Vertex AI 모드 활성화
        return Client.builder()
                .project(projectId)
                .location(location)
                .credentials(creds)
                .vertexAI(true)
                .build();
    }
}
