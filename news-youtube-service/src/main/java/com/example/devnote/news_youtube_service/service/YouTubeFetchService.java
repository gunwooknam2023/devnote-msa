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

    /** Kafka에 메시지 발행을 위한 템플릿 */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Youtube 클라이언트 */
    private final YouTube youtubeclient;

    /** 조회할 카테고리 목록 */
    @Value("#{'${youtube.categories}'.split(',')}")
    private List<String> categories;

    /** 조회할 채널 ID 목록 */
    @Value("#{'${youtube.channelIds}'.split(',')}")
    private List<String> channelIds;

    /**
     * 전체 수집 스케줄러
     * - fixedRate: application.yml의 youtube.fetch.rate 값 기준으로 실행
     */
    @Scheduled(fixedRateString = "${youtube.fetch.rate}")
    public void fetchAndPublishYoutube() {
        categories.forEach(this::fetchByCategory);
        channelIds.forEach(this::fetchByChannel);
    }

    /**
     * 특정 카테고리를 키워드로 YouTube 검색 후 결과 발행
     */
    private void fetchByCategory(String category) {
        log.info("Start fetching YOUTUBE for category={}", category);
        try {
            // 검색 요청 설정: snippet만 조회, 최대 N개(현재 1)
            var request = youtubeclient.search()
                    .list("snippet")
                    .setKey(apiKey)
                    .setQ("개발 " + category)
                    .setType("video")
                    .setMaxResults(1L);

            List<SearchResult> items = request.execute().getItems();

            // 각 검색 결과에 대해 공통 발행 로직 실행
            items.forEach(r -> {
                var sn = r.getSnippet();
                publishContent(
                        /*category*/      "TBC",
                        /*videoId*/       r.getId().getVideoId(),
                        /*channelId*/     sn.getChannelId(),
                        /*channelTitle*/  sn.getChannelTitle(),
                        /*title*/         sn.getTitle(),
                        /*thumbUrl*/      sn.getThumbnails().getHigh().getUrl(),
                        /*publishedAt*/   Instant.parse(sn.getPublishedAt().toStringRfc3339())
                );
            });
            log.info("Completed fetching YOUTUBE for category={}, count={}", category, items.size());
        } catch (Exception ex) {
            log.error("Failed fetching/publishing YOUTUBE for category={}", category, ex);
        }
    }

    /**
     * 특정 채널의 업로드 플레이리스트를 조회 후 모든 영상 발행
     */
    private void fetchByChannel(String channelId) {
        log.info("Start fetching YOUTUBE uploads for channel={}", channelId);
        try {
            // 채널 contentDetails 조회 → 업로드 플레이리스트 ID 얻기
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

            // 플레이리스트 영상 조회 (최대 50개, 현재 50개)
            var plReq = youtubeclient.playlistItems()
                    .list("snippet,contentDetails")
                    .setKey(apiKey)
                    .setPlaylistId(uploadsPlaylistId)
                    .setMaxResults(50L);
            List<PlaylistItem> items = plReq.execute().getItems();

            // 각 검색 결과에 대해 공통 발행 로직 실행
            items.forEach(pi -> {
                var sn = pi.getSnippet();
                String videoId = pi.getContentDetails().getVideoId();
                publishContent(
                        /*category*/      "TBC",
                        /*videoId*/       videoId,
                        /*channelId*/     channelId,
                        /*channelTitle*/  sn.getChannelTitle(),
                        /*title*/         sn.getTitle(),
                        /*thumbUrl*/      sn.getThumbnails().getHigh().getUrl(),
                        /*publishedAt*/   Instant.parse(sn.getPublishedAt().toStringRfc3339())
                );
            });
            log.info("Completed fetching uploads for channel={}", channelId);
        } catch (Exception ex) {
            log.error("Failed fetching/publishing uploads for channel={}", channelId, ex);
        }
    }

    /**
     * 공통 발행 로직 메서드
     * 1) 조회수 조회(fetchViewCount)
     * 2) 채널 썸네일 조회(fetchChannelThumbnail)
     * 3) ContentMessageDto 빌드
     * 4) Kafka에 발행
     *
     * @param category          발행 키(파티션 기준)
     * @param videoId           YouTube 영상 ID
     * @param channelId         채널 ID
     * @param channelTitle      채널명
     * @param title             영상 제목
     * @param thumbnailUrl      영상 썸네일 URL
     * @param publishedAt       게시 일시 (RFC3339)
     */
    private void publishContent(
            String category,
            String videoId,
            String channelId,
            String channelTitle,
            String title,
            String thumbnailUrl,
            Instant publishedAt
    ) {
        // 1) 조회수 정보 가져오기
        Long viewCount = fetchViewCount(videoId);

        // 2) 채널 썸네일 URL 가져오기
        String channelThumb = fetchChannelThumbnail(channelId);

        // 3) DTO 생성
        ContentMessageDto msg = ContentMessageDto.builder()
                .source("YOUTUBE")
                .category(category)
                .title(title)
                .link("https://www.youtube.com/watch?v=" + videoId)
                .thumbnailUrl(thumbnailUrl)
                .publishedAt(publishedAt)
                .channelTitle(channelTitle)
                .channelThumbanilUrl(channelThumb)
                .viewCount(viewCount)
                .build();

        // 4) Kafka 토픽에 전송 (토픽명, 파티션 키, 메시지)
        kafkaTemplate.send(
                KafkaProducerConfig.topicRawContent(),
                category,
                msg
        );
        log.debug("Published YOUTUBE msg: category={}, videoId={}", category, videoId);
    }

    /**
     * YouTube 영상의 통계 API를 통해 조회수를 가져오기
     * @param videoId YouTube 영상 ID
     * @return 조회수(Long) 또는 예외 시 null
     */
    private Long fetchViewCount(String videoId) {
        try {
            var statsReq = youtubeclient.videos()
                    .list("statistics")
                    .setKey(apiKey)
                    .setId(videoId);
            return statsReq.execute().getItems().stream()
                    .findFirst()
                    .map(item -> item.getStatistics().getViewCount().longValue())
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Failed to fetch statistics for video {}: {}", videoId, ex.getMessage());
            return null;
        }
    }

    /**
     * 채널 정보 API를 통해 채널의 썸네일 URL을 가져오기
     * @param channelId YouTube 채널 ID
     * @return 채널 썸네일 URL 또는 예외/미존재 시 null
     */
    private String fetchChannelThumbnail(String channelId) {
        try {
            var channelReq = youtubeclient.channels()
                    .list("snippet")
                    .setKey(apiKey)
                    .setId(channelId);
            var list = channelReq.execute().getItems();
            if (!list.isEmpty()) {
                return list.get(0)
                        .getSnippet()
                        .getThumbnails()
                        .getHigh()
                        .getUrl();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch channel thumbnail for {}: {}", channelId, ex.getMessage());
        }
        return null;
    }
}
