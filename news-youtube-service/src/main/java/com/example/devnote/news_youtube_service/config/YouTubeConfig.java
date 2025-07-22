package com.example.devnote.news_youtube_service.config;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.youtube.YouTube;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * YOuTube Data API Client 빈 등록
 */
@Configuration
public class YouTubeConfig {
    @Value("${youtube.api.key}")
    private String apiKey;

    @Bean
    public YouTube youtubeClient() {
        // 모든 요청에 API 키를 자동 삽입
        HttpRequestInitializer initializer = request -> {
            // 헤더 방식으로 인증 정보 설정
            request.getHeaders().set("X-Goog-Api-Key", apiKey);
        };

        return new YouTube.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                initializer
        )
                .setApplicationName("devnote-news-youtube-service")
                .build();
    }
}
