package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.config.KafkaProducerConfig;
import com.example.devnote.news_youtube_service.dto.ContentMessageDto;
import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * YouTube 데이터를 주기적으로 수집하여 Kafka 에 발행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeFetchService {
    @Value("${youtube.api.key}")
    private String apiKey;

    private final ChannelSubscriptionRepository channelSubscriptionRepository;

    /** Kafka에 메시지 발행을 위한 템플릿 */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Youtube 클라이언트 */
    private final YouTube youtubeclient;

    /** 레디스 */
    private final StringRedisTemplate redis;

    /** 조회할 카테고리 목록 */
    @Value("#{'${youtube.categories}'.split(',')}")
    private List<String> categories;

    /**
     * 1시간마다 실행되는 메인 스케줄러
     * 1) 카테고리 검색 (Redis + publishedAfter)
     * 2) 채널 구독: 초기 전체 로딩 → RSS 증분 로딩
     */
    @Scheduled(fixedRateString = "${youtube.fetch.rate}")
    public void fetchAndPublishYoutube() {
        // 1) 카테고리 기반 검색
        categories.forEach(this::fetchByCategory);

        // 2) 채널 구독 로직
        // 2-1) initialLoaded == false → 전체 로딩
        List<ChannelSubscription> toInit = channelSubscriptionRepository.findByInitialLoadedFalse();
        toInit.forEach(sub -> {
            fetchAllByChannel(sub);
            sub.setInitialLoaded(true);
            channelSubscriptionRepository.save(sub);
        });
        // 2-2) initialLoaded == true → RSS 기반 증분 로딩
        List<ChannelSubscription> toRss = channelSubscriptionRepository.findByInitialLoadedTrue();
        toRss.forEach(this::fetchRssByChannel);
    }

    /** 1) 카테고리 검색: Redis 에 마지막 수집 시각 저장 */
    private void fetchByCategory(String category) {
        String redisKey = "yt:lastFetched:cat:" + category;
        Long lastTs = redis.opsForValue().get(redisKey) != null
                ? Long.parseLong(redis.opsForValue().get(redisKey))
                : Instant.now().minusSeconds(24 * 3600).toEpochMilli(); // 초기 24시간 전

        log.info("[Category] '{}' since={}", category, Instant.ofEpochMilli(lastTs));

        try {
            var req = youtubeclient.search()
                    .list("snippet")
                    .setKey(apiKey)
                    .setQ("개발 " + category)
                    .setType("video")
                    .setOrder("date")
                    .setMaxResults(10L)
                    .setPublishedAfter(new DateTime(lastTs));

            List<SearchResult> items = req.execute().getItems();
            if (items.isEmpty()) {
                log.info("  – No new videos for '{}'", category);
                return;
            }

            // 신규 영상에 대해 발행
            items.forEach(r -> {
                var sn = r.getSnippet();
                Instant published = Instant.parse(sn.getPublishedAt().toStringRfc3339());
                publishContent(
                        "TBC",
                        r.getId().getVideoId(),
                        sn.getChannelId(),
                        sn.getChannelTitle(),
                        sn.getTitle(),
                        sn.getThumbnails().getHigh().getUrl(),
                        published
                );
            });

            // 최신 타임스탬프 갱신
            long newest = items.stream()
                    .map(r -> Instant.parse(r.getSnippet().getPublishedAt().toStringRfc3339())
                            .toEpochMilli())
                    .max(Comparator.naturalOrder())
                    .get();
            redis.opsForValue().set(redisKey, String.valueOf(newest));
            log.info("  ✓ Updated lastFetched for '{}' → {}", category, Instant.ofEpochMilli(newest));

        } catch (Exception ex) {
            log.error("Failed category fetch '{}': {}", category, ex.getMessage(), ex);
        }
    }

    /** 2-1) 채널 초기 전체 로딩: playlistItems.list + 페이징 순회 */
    private void fetchAllByChannel(ChannelSubscription sub) {
        String channelId = sub.getChannelId();
        log.info("[FullLoad] channel={}", channelId);

        try {
            // 1) 업로드 전용 플레이리스트 ID 조회
            var chResp = youtubeclient.channels()
                    .list("contentDetails")
                    .setKey(apiKey)
                    .setId(channelId)
                    .execute();
            String uploadsPlaylistId = chResp.getItems()
                    .get(0)
                    .getContentDetails()
                    .getRelatedPlaylists()
                    .getUploads();

            // 2) 페이지 단위로 순회
            String pageToken = null;
            do {
                var plResp = youtubeclient.playlistItems()
                        .list("snippet,contentDetails")
                        .setKey(apiKey)
                        .setPlaylistId(uploadsPlaylistId)
                        .setMaxResults(50L)
                        .setPageToken(pageToken)
                        .execute();

                for (PlaylistItem pi : plResp.getItems()) {
                    Instant published = Instant.parse(
                            pi.getSnippet().getPublishedAt().toStringRfc3339());
                    publishContent(
                            "TBC",
                            pi.getContentDetails().getVideoId(),
                            pi.getSnippet().getChannelId(),
                            pi.getSnippet().getChannelTitle(),
                            pi.getSnippet().getTitle(),
                            pi.getSnippet().getThumbnails().getHigh().getUrl(),
                            published
                    );
                }

                pageToken = plResp.getNextPageToken();
            } while (pageToken != null);

        } catch (Exception ex) {
            log.error("Failed full-load for channel {}: {}", channelId, ex.getMessage(), ex);
        }
    }

    /** 2-2) 채널 RSS 증분 로딩: 최신 약 30개만 파싱 */
    private void fetchRssByChannel(ChannelSubscription sub) {
        String channelId = sub.getChannelId();
        String feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
        log.info("[RSSLoad] channel={} feed={}", channelId, feedUrl);

        try (XmlReader reader = new XmlReader(new URL(feedUrl))) {
            SyndFeed feed = new SyndFeedInput().build(reader);

            for (SyndEntry entry : feed.getEntries()) {
                // 1) videoId 추출
                String videoId = entry.getForeignMarkup().stream()
                        .filter(n -> "videoId".equals(n.getName()))
                        .map(n -> n.getValue())
                        .findFirst()
                        .orElse(null);
                if (videoId == null) continue;

                // 2) YouTube Data API 로 Snippet+Statistics 조회
                var resp = youtubeclient.videos()
                        .list("snippet")
                        .setKey(apiKey)
                        .setId(videoId)
                        .execute();

                if (resp.getItems().isEmpty()) continue;
                var video = resp.getItems().get(0);
                var sn = video.getSnippet();

                // 3) 필요한 정보 추출
                String channelTitle = sn.getChannelTitle();
                String title        = sn.getTitle();
                String thumbnailUrl = sn.getThumbnails().getHigh().getUrl();
                Instant publishedAt = entry.getPublishedDate() != null
                        ? entry.getPublishedDate().toInstant()
                        : Instant.now();

                // 4) Kafka 발행
                publishContent(
                        "TBC",
                        videoId,
                        channelId,
                        channelTitle,
                        title,
                        thumbnailUrl,
                        publishedAt
                );
            }
        } catch (Exception ex) {
            log.error("Failed RSS-load for channel {}: {}", channelId, ex.getMessage(), ex);
        }
    }

    /** 공통 Kafka 발행 메서드 */
    private void publishContent(
            String category,
            String videoId,
            String channelId,
            String channelTitle,
            String title,
            String thumbnailUrl,
            Instant publishedAt
    ) {
        // 1) 영상 조회수 + 영상 길이 조회
        VideoStats stats = fetchVideoStats(videoId);
        Long viewCount = stats.viewCount();
        long durationSec = stats.duration() != null ? stats.duration().getSeconds() : 0;
        String form = (stats.duration() != null && stats.duration().getSeconds() <= 60) ? "SHORTS" : "LONGFORM";

        // 2) 채널 썸네일 조회
        String channelThumb = ensureChannelThumbnail(channelId, channelTitle);

        ContentMessageDto msg = ContentMessageDto.builder()
                .source("YOUTUBE")
                .category(category)
                .title(title)
                .link("https://www.youtube.com/watch?v=" + videoId)
                .thumbnailUrl(thumbnailUrl)
                .publishedAt(publishedAt)
                .channelTitle(channelTitle)
                .channelThumbnailUrl(channelThumb)
                .viewCount(viewCount)
                .durationSeconds(durationSec)
                .videoForm(form)
                .build();

        kafkaTemplate.send(
                KafkaProducerConfig.TOPIC_RAW_CONTENT,
                category,
                msg
        );
        log.debug("▶ Published {} / {}", category, videoId);
    }

    /**
     * 채널 썸네일 저장
     * DB에 channelId가 없으면 새 구독 레코드 생성
     * youtubeName에는 API에서 받은 channelTitle 넣고,
     * initialLoaded는 true 상태로 저장,
     * channelThumbnailUrl은 API에서 받아 저장
     */
    private String ensureChannelThumbnail(String channelId, String channelTitle) {
        Optional<ChannelSubscription> opt = channelSubscriptionRepository.findByChannelId(channelId);
        if(opt.isPresent() && opt.get().getChannelThumbnailUrl() != null) {
            return opt.get().getChannelThumbnailUrl();
        }
        try {
            // API 호출로 썸네일 정보 가져오기
            var resp = youtubeclient.channels()
                    .list("snippet, statistics")
                    .setKey(apiKey)
                    .setId(channelId)
                    .execute();
           // 썸네일
           var item = resp.getItems().get(0);
           var snippet = item.getSnippet();
           String thumb = snippet.getThumbnails().getHigh().getUrl();


           var stats = item.getStatistics();
           Long subsCount = stats.getSubscriberCount() != null
                   ? stats.getSubscriberCount().longValue() : 0L;

            // DB에 새 레코드 생성 또는 기존 업데이트
            ChannelSubscription sub = opt.orElseGet(() ->
                    ChannelSubscription.builder()
                            .youtubeName(channelTitle)
                            .channelId(channelId)
                            .initialLoaded(true)
                            .channelThumbnailUrl(thumb)
                            .subscriberCount(subsCount)
                            .build()
            );
            sub.setSubscriberCount(subsCount);
            sub.setChannelThumbnailUrl(thumb);
            channelSubscriptionRepository.save(sub);
            return thumb;
        } catch (Exception ex) {
            log.warn("Failed to fetch & save thumbnail for {}: {}", channelId, ex.getMessage());
            return null;
        }
    }

    /** 영상 조회수 + 영상 길이 조회 */
    private record VideoStats(Long viewCount, Duration duration) {}
    private VideoStats fetchVideoStats(String videoId) {
        try {
            // 1) statistics,contentDetails 요청
            var req = youtubeclient.videos()
                    .list("statistics,contentDetails")
                    .setKey(apiKey)
                    .setId(videoId);

            // 2) API 호출 및 첫 번째 결과 추출
            Video item = req.execute().getItems().stream().findFirst().orElse(null);

            // 3) 결과가 없으면 null 필드로 VideoStats 생성
            if (item == null) return new VideoStats(null, null);

            // 4) 조회수 가져오기
            VideoStatistics st = item.getStatistics();
            VideoContentDetails cd = item.getContentDetails();

            // 5) duration 문자열 파싱
            Duration dur = Duration.parse(cd.getDuration());

            // 6) VideoStats 객체에 담아 반환
            return new VideoStats(st.getViewCount().longValue(), dur);
        } catch (Exception ex) {
            log.warn("Failed to fetch stats for {}: {}", videoId, ex.getMessage());
            return new VideoStats(null, null);
        }
    }
}
