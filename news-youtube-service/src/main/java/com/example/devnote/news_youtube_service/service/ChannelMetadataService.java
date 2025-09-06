package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelMetadataService {

    private final ChannelSubscriptionRepository channelRepository;
    private final YouTube youtubeClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    // 서비스 시작 시 최초 1회 실행
    @PostConstruct
    public void initialMetadataUpdate() {
        log.info("Triggering initial channel metadata update on application startup...");
        updateAllChannelMetadata();
    }

    /**
     * 매주 일요일 새벽 3시에 실행되는 스케줄러
     * 모든 유튜브 채널의 최신 메타데이터(이름, 썸네일, 구독자 수)를 갱신
     */
    @Scheduled(cron = "0 0 3 * * SUN") // 매주 일요일 새벽 3시에 실행
    @Transactional
    public void updateAllChannelMetadata() {
        log.info("Starting weekly channel metadata update job...");

        // 1. DB에서 'YOUTUBE' 소스인 모든 채널을 조회
        List<ChannelSubscription> allYoutubeChannels = channelRepository.findBySource("YOUTUBE");
        if (allYoutubeChannels.isEmpty()) {
            log.info("No YouTube channels found to update. Job finished.");
            return;
        }

        // 2. API 할당량을 아끼기 위해 50개씩 묶어서 처리
        List<List<ChannelSubscription>> batches = Lists.partition(allYoutubeChannels, 50);
        int updatedCount = 0;

        for (List<ChannelSubscription> batch : batches) {
            try {
                // 3. 현재 배치의 채널 ID 목록을 추출
                List<String> channelIdList = batch.stream()
                        .map(ChannelSubscription::getChannelId)
                        .collect(Collectors.toList());

                String commaSeparatedIds = String.join(",", channelIdList);

                // 4. YouTube Data API를 한번만 호출하여 50개 채널의 정보를 모두 조회
                List<Channel> apiResult = youtubeClient.channels()
                        .list("snippet,statistics")
                        .setId(commaSeparatedIds)
                        .setKey(apiKey)
                        .execute()
                        .getItems();

                // 5. 결과를 ID 기준으로 빠르게 찾을 수 있도록 Map으로 변환
                Map<String, Channel> apiResultMap = apiResult.stream()
                        .collect(Collectors.toMap(Channel::getId, Function.identity()));

                List<ChannelSubscription> channelsToUpdate = new ArrayList<>();

                // 6. DB 데이터와 API 데이터를 비교하여 변경된 내용이 있거나, 기존 정보가 누락된 경우 업데이트
                for (ChannelSubscription dbChannel : batch) {
                    Channel apiChannel = apiResultMap.get(dbChannel.getChannelId());
                    if (apiChannel == null) continue; // API 결과에 없는 채널은 건너뛰기

                    boolean isChanged = false;

                    // 이름(title) 비교 및 업데이트
                    String newName = apiChannel.getSnippet().getTitle();
                    if (!Objects.equals(dbChannel.getYoutubeName(), newName)) {
                        dbChannel.setYoutubeName(newName);
                        isChanged = true;
                    }

                    // 썸네일 비교 및 업데이트 (누락된 경우 채워넣기)
                    String newThumbnail = apiChannel.getSnippet().getThumbnails().getHigh().getUrl();
                    if (!Objects.equals(dbChannel.getChannelThumbnailUrl(), newThumbnail)) {
                        dbChannel.setChannelThumbnailUrl(newThumbnail);
                        isChanged = true;
                    }

                    // 구독자 수 비교 및 업데이트 (누락된 경우 채워넣기)
                    Long newSubsCount = apiChannel.getStatistics().getSubscriberCount().longValue();
                    if (!Objects.equals(dbChannel.getSubscriberCount(), newSubsCount)) {
                        dbChannel.setSubscriberCount(newSubsCount);
                        isChanged = true;
                    }

                    if (isChanged) {
                        channelsToUpdate.add(dbChannel);
                    }
                }

                // 7. 변경된 채널 정보들을 DB에 일괄 저장
                if (!channelsToUpdate.isEmpty()) {
                    channelRepository.saveAll(channelsToUpdate);
                    updatedCount += channelsToUpdate.size();
                    log.info("Updated metadata for {} channels in this batch.", channelsToUpdate.size());
                }

            } catch (Exception e) {
                log.error("Failed to update channel metadata batch.", e);
            }
        }
        log.info("Finished weekly channel metadata update job. Total updated channels: {}", updatedCount);
    }
}