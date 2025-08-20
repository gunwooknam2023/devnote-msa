package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.dto.NewsProperties;
import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsSourceInitializer implements CommandLineRunner {

    private final ChannelSubscriptionRepository channelSubscriptionRepository;
    private final NewsProperties newsProperties;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking for news sources in the database...");
        int newSourcesCount = 0;

        // yml에 정의된 모든 언론사에 대해 반복
        for (NewsProperties.Source sourceProp : newsProperties.getSources()) {
            // 뉴스 언론사를 위한 고유 channelId 생성 (예: "news-보안뉴스")
            String newsChannelId = "news-" + sourceProp.getName();

            // 이 channelId로 DB에 이미 존재하는지 확인
            if (channelSubscriptionRepository.findByChannelId(newsChannelId).isEmpty()) {

                // 존재하지 않을 경우에만 새로 생성하여 저장
                ChannelSubscription newsChannel = ChannelSubscription.builder()
                        .youtubeName(sourceProp.getName())
                        .channelId(newsChannelId) // 생성된 고유 ID 사용
                        .channelThumbnailUrl(sourceProp.getThumbnailUrl())
                        .source("NEWS")
                        .initialLoaded(true) // 뉴스는 이 플래그를 사용하지 않으므로 true로 고정
                        .subscriberCount(null) // 구독자 수는 null
                        .build();

                channelSubscriptionRepository.save(newsChannel);
                newSourcesCount++;
                log.info("Added new news source to DB: {}", sourceProp.getName());
            }
        }

        if (newSourcesCount > 0) {
            log.info("Successfully initialized {} new news sources.", newSourcesCount);
        } else {
            log.info("All news sources from yml already exist in the database. Skipping initialization.");
        }
    }
}
