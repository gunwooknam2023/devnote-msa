package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.config.KafkaProducerConfig;
import com.example.devnote.news_youtube_service.dto.ContentMessageDto;
import com.example.devnote.news_youtube_service.dto.NewsProperties;
import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 뉴스 데이터를 주기적으로 수집하여 Kafka 에 발행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsFetchService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ChannelSubscriptionRepository channelSubscriptionRepository;
    private final NewsProperties newsProperties;

    /**
     * DB에 저장된 모든 뉴스 언론사의 피드를 주기적으로 수집
     */
    @Scheduled(fixedDelayString = "3600000")
    public void fetchAndPublishNews() {
        // 1. DB에서 source가 'NEWS'인 모든 언론사 정보를 가져옴
        List<ChannelSubscription> newsSources = channelSubscriptionRepository.findBySource("NEWS");
        if (newsSources.isEmpty()) {
            log.warn("No news sources found in the database.");
            return;
        }

        // 2. application.yml에서 피드 정보를 읽어와 Map으로 변환
        Map<String, List<String>> feedsMap = newsProperties.getSources().stream()
                .collect(Collectors.toMap(NewsProperties.Source::getName, NewsProperties.Source::getFeeds));

        log.info("▶ Starting news fetch for {} sources from database.", newsSources.size());

        // 3. DB에서 가져온 각 언론사에 대해 뉴스 수집 실행
        newsSources.forEach(source -> {
            String sourceName = source.getYoutubeName();
            List<String> feedUrls = feedsMap.get(sourceName);

            if (feedUrls == null || feedUrls.isEmpty()) {
                log.warn("  ! No feed URLs found for '{}' in application.yml. Skipping.", sourceName);
                return;
            }

            log.info("  ▶ Fetching news for '{}'", sourceName);

            // 4. 해당 언론사의 모든 RSS 피드 주소에 대해 수집 실행
            feedUrls.forEach(feedUrl ->
                    fetchFromUrl(feedUrl, source.getYoutubeName(), source.getChannelThumbnailUrl(), source.getChannelId())
            );
        });

        log.info("✔ Completed news fetch cycle");
    }

    /**
     * 주어진 RSS URL 에 요청을 보내고, 파싱한 엔트리를 Kafka 로 발행
     * @param url RSS 피드 주소
     * @param sourceName 언론사 이름
     * @param sourceThumbnailUrl 언론사 로고 URL
     * @param sourceChannelId 언론사의 고유 Channel ID
     */
    private void fetchFromUrl(String url, String sourceName, String sourceThumbnailUrl, String sourceChannelId) {
        log.info("    • Fetching feed: {}", url);
        try (XmlReader reader = new XmlReader(new URL(url))) {
            SyndFeed feed = new SyndFeedInput().build(reader);

            var items = feed.getEntries().stream()
                    .filter(distinctByKey(e -> e.getTitle() + "::" + e.getLink()))
                    .map(entry -> toDto(entry, sourceName, sourceThumbnailUrl, sourceChannelId))
                    .peek(dto -> dto.setSource("NEWS"))
                    .collect(Collectors.toList());

            items.forEach(dto ->
                    kafkaTemplate.send(KafkaProducerConfig.topicRawContent(), dto)
            );
            log.info("    ✓ Published {} items for '{}'", items.size(), sourceName);

        } catch (Exception ex) {
            log.error("    ✘ Failed to fetch RSS: {}", url, ex);
        }
    }

    /**
     * SyndEntry → ContentMessageDto 변환
     */
    private ContentMessageDto toDto(SyndEntry entry, String sourceName, String sourceThumbnailUrl, String sourceChannelId) {
        Instant published = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant()
                : Instant.now();

        String thumbnail = entry.getEnclosures().stream()
                .findFirst()
                .map(enc -> enc.getUrl())
                .orElse(null);

        return ContentMessageDto.builder()
                .title(entry.getTitle())
                .link(entry.getLink())
                .description(entry.getDescription() != null
                        ? entry.getDescription().getValue()
                        : null)
                .thumbnailUrl(thumbnail)
                .publishedAt(published)
                .category("TBC")
                .channelTitle(sourceName)
                .channelThumbnailUrl(sourceThumbnailUrl)
                .channelId(sourceChannelId)
                .build();
    }

    /**
     * 스트림에서 키 추출 함수(keyExtractor) 기준으로 중복을 제거하는 Predicate 반환
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        var seen = java.util.Collections.newSetFromMap(
                new java.util.concurrent.ConcurrentHashMap<>()
        );
        return t -> seen.add(keyExtractor.apply(t));
    }
}