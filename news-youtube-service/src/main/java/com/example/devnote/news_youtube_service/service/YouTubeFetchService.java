package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.config.KafkaProducerConfig;
import com.example.devnote.news_youtube_service.dto.ContentMessageDto;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * YouTube 데이터를 주기적으로 수집하여 Kafka 에 발행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeFetchService {
    @Value("${youtube.api.key}")
    private String apiKey;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final YouTube youtubeclient;

    /** 수집할 소주제 리스트 */
    @Value("#{'${youtube.categories}'.split(',')}")
    private List<String> categories;

    /** 1시간마다 실행 */
    @Scheduled(fixedRateString = "${youtube.fetch.rate}")
    public void fetchAndPublishYoutube() {
        categories.forEach(this::fetchByCategory);
    }

    private void fetchByCategory(String category) {
        log.info("Start fetching YOUTUBE for category={}", category);
        try {
            var request = youtubeclient.search()
                    .list("snippet")
                    .setKey(apiKey)
                    .setQ("개발 " + category)
                    .setType("video")
                    .setMaxResults(50L);

            List<SearchResult> items = request.execute().getItems();
            for (SearchResult r : items) {
                var sn = r.getSnippet();
                String channelId = sn.getChannelId();
                String channelTitle = sn.getChannelTitle();

                // 채널 썸네일
                String channelThumbnailUrl = null;
                try {
                    var channelReq = youtubeclient.channels()
                            .list("snippet")
                            .setKey(apiKey)
                            .setId(channelId);

                    var channelList = channelReq.execute().getItems();

                    if (!channelList.isEmpty()) {
                        channelThumbnailUrl = channelList.get(0)
                                .getSnippet()
                                .getThumbnails()
                                .getDefault()
                                .getUrl();
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch channel thumbnail for {}: {}", channelId, ex.getMessage());
                }

                ContentMessageDto msg = ContentMessageDto.builder()
                        .source("YOUTUBE")
                        .category(category)
                        .title(sn.getTitle())
                        .link("https://www.youtube.com/watch?v=" + r.getId().getVideoId())
                        .thumbnailUrl(sn.getThumbnails().getDefault().getUrl())
                        .publishedAt(Instant.parse(sn.getPublishedAt().toStringRfc3339()))
                        .channelTitle(channelTitle)
                        .channelThumbanilUrl(channelThumbnailUrl)
                        .build();

                kafkaTemplate.send(
                        KafkaProducerConfig.topicRawContent(),
                        category,
                        msg
                );
                log.debug("Published YOUTUBE msg: category={}, videoId={}", category, r.getId().getVideoId());
            }
            log.info("Completed fetching YOUTUBE for category={}, count={}", category, items.size());

        } catch (Exception ex) {
            log.error("Failed fetching/publishing YOUTUBE for category={}", category, ex);
        }
    }
}
