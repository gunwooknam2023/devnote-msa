package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.config.YouTubeProperties;
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
public class YouTubeChannelInitializer implements CommandLineRunner {

    private final ChannelSubscriptionRepository channelSubscriptionRepository;
    private final YouTubeProperties youTubeProperties;
    private final YouTubeFetchService youTubeFetchService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking for YouTube channels in the database...");
        int newChannelsCount = 0;

        // yml에 정의된 모든 채널에 대해 반복
        for (YouTubeProperties.Channel channelProp : youTubeProperties.getChannels()) {

            // 이 channelId로 DB에 이미 존재하는지 확인
            if (channelSubscriptionRepository.findByChannelId(channelProp.getChannelId()).isEmpty()) {

                // 존재하지 않을 경우에만 새로 생성하여 저장
                ChannelSubscription newChannel = ChannelSubscription.builder()
                        .youtubeName(channelProp.getName())
                        .channelId(channelProp.getChannelId())
                        .source("YOUTUBE")
                        .initialLoaded(false)
                        .build();

                channelSubscriptionRepository.save(newChannel);
                newChannelsCount++;
                log.info("Added new YouTube channel to DB: {}", channelProp.getName());
            }
        }

        log.info("Triggering initial YouTube data fetch...");
        youTubeFetchService.fetchAndPublishYoutube();
        log.info("Initial YouTube data fetch finished.");

        if (newChannelsCount > 0) {
            log.info("Successfully initialized {} new YouTube channels.", newChannelsCount);
        } else {
            log.info("All YouTube channels from yml already exist in the database. Skipping initialization.");
        }
    }
}