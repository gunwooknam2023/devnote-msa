package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.config.KafkaProducerConfig;
import com.example.devnote.news_youtube_service.dto.ContentMessageDto;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
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

    /** 수집할 팔로우 채널 ID 목록 */
    @Value("#{'${youtube.channelIds}'.split(',')}")
    private List<String> channelIds;

    /** 1시간마다 실행 */
    @Scheduled(fixedRateString = "${youtube.fetch.rate}")
    public void fetchAndPublishYoutube() {
        categories.forEach(this::fetchByCategory);
        channelIds.forEach(this::fetchByChannel);
    }

    private void fetchByCategory(String category) {
        log.info("Start fetching YOUTUBE for category={}", category);
        try {
            var request = youtubeclient.search()
                    .list("snippet")
                    .setKey(apiKey)
                    .setQ("개발 " + category)
                    .setType("video")
                    .setMaxResults(1L);

            List<SearchResult> items = request.execute().getItems();
            for (SearchResult r : items) {
                var sn = r.getSnippet();
                String videoId = r.getId().getVideoId();
                String channelId = sn.getChannelId();
                String channelTitle = sn.getChannelTitle();

                // 영상 통계(조회수) 가져오기
                Long viewCount = null;
                try {
                    var statsReq = youtubeclient.videos()
                            .list("statistics")
                            .setKey(apiKey)
                            .setId(videoId);
                    var statsItemOpt = statsReq.execute().getItems().stream().findFirst();
                    if (statsItemOpt.isPresent()) {
                        BigInteger countBI = statsItemOpt.get()
                                .getStatistics()
                                .getViewCount();
                        // BigInteger → Long 변환
                        viewCount = (countBI != null ? countBI.longValue() : null);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch statistics for video {}: {}", videoId, ex.getMessage());
                }

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
                        .viewCount(viewCount)
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

    private void fetchByChannel(String channelId) {
        log.info("Start fetching YOUTUBE uploads for channel={}", channelId);
        try {
            // uploads 플레이리스트 ID 조회
            var chReq   = youtubeclient.channels()
                    .list("contentDetails")
                    .setKey(apiKey)
                    .setId(channelId);
            var chItems = chReq.execute().getItems();
            if (chItems.isEmpty()) {
                log.warn("No channel data for {}", channelId);
                return;
            }
            String uploadsPlaylistId = chItems.get(0)
                    .getContentDetails()
                    .getRelatedPlaylists()
                    .getUploads();

            // 플레이리스트 아이템(업로드 영상) 조회
            var plReq   = youtubeclient.playlistItems()
                    .list("snippet,contentDetails")
                    .setKey(apiKey)
                    .setPlaylistId(uploadsPlaylistId)
                    .setMaxResults(50L);
            List<PlaylistItem> items = plReq.execute().getItems();

            for (PlaylistItem pi : items) {
                var sn = pi.getSnippet();
                String videoId      = pi.getContentDetails().getVideoId();
                String channelTitle = sn.getChannelTitle();

                // 영상 통계(조회수) 가져오기
                Long viewCount = null;
                try {
                    var statsReq = youtubeclient.videos()
                            .list("statistics")
                            .setKey(apiKey)
                            .setId(videoId);
                    var statsOpt = statsReq.execute().getItems().stream().findFirst();
                    if (statsOpt.isPresent()) {
                        viewCount = statsOpt.get()
                                .getStatistics()
                                .getViewCount()
                                .longValue();
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch statistics for video {}: {}", videoId, ex.getMessage());
                }

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

                // 메시지 빌드 (category만 "TBC" 로 고정)
                ContentMessageDto msg = ContentMessageDto.builder()
                        .source("YOUTUBE")
                        .category("TBC")
                        .title(sn.getTitle())
                        .link("https://www.youtube.com/watch?v=" + videoId)
                        .thumbnailUrl(sn.getThumbnails().getDefault().getUrl())
                        .publishedAt(Instant.parse(sn.getPublishedAt().toStringRfc3339()))
                        .channelTitle(channelTitle)
                        .channelThumbanilUrl(channelThumbnailUrl)
                        .viewCount(viewCount)
                        .build();

                kafkaTemplate.send(
                        KafkaProducerConfig.topicRawContent(),
                        "TBC",
                        msg
                );
                log.debug("Published YOUTUBE upload: channel={}, videoId={}", channelId, videoId);
            }

            log.info("Completed fetching uploads for channel={}", channelId);

        } catch (Exception ex) {
            log.error("Failed fetching/publishing uploads for channel={}", channelId, ex);
        }
    }
}
