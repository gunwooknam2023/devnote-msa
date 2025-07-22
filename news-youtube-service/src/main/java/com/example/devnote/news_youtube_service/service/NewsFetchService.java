package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.config.KafkaProducerConfig;
import com.example.devnote.news_youtube_service.dto.ContentMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * 뉴스 데이터를 주기적으로 수집하여 Kafka 에 발행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsFetchService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 수집할 소주제 리스트 */
    @Value("#{'${news.categories}'.split(',')}")
    private List<String> categories;

    /** 30분마다 실행 */
    @Scheduled(fixedDelayString = "${news.fetch.delay}")
    public void fetchAndPublishNews() {
        categories.forEach(this::fetchByCategory);
    }

    private void fetchByCategory(String category) {
        log.info("Start fetching NEWS for category={}", category);
        try {
            String query = URLEncoder.encode("개발 " + category, StandardCharsets.UTF_8);
            List<ContentMessageDto> resultList = List.of(
                    ContentMessageDto.builder()
                            .source("NEWS")
                            .category(category)
                            .title("예시 뉴스 제목")
                            .link("https://news.example.com/1")
                            .thumbnailUrl(null)
                            .publishedAt(Instant.now())
                            .build()
            );

            for (ContentMessageDto item : resultList) {
                kafkaTemplate.send(
                        KafkaProducerConfig.topicRawContent(),
                        category,
                        item
                );
                log.debug("Published NEWS msg: category={}, title={}", category, item.getTitle());
            }
            log.info("Completed fetching NEWS for category={}, count={}", category, resultList.size());

        } catch (Exception ex) {
            log.error("Failed fetching/publishing NEWS for category={}", category, ex);
        }
    }
}
