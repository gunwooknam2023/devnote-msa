package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.config.KafkaProducerConfig;
import com.example.devnote.news_youtube_service.dto.ContentMessageDto;
import com.example.devnote.news_youtube_service.dto.NewsProperties;
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
    private final NewsProperties props;

    /**
     * 고정 피드 + boannews mkind 피드를 1시간마다 수집
     */
    @Scheduled(fixedDelayString = "#{@newsProperties.fetchDelay}")
    public void fetchAndPublishNews() {
        var feeds    = props.getFeeds();
        var boanBase = props.getBoan().getBaseUrl();
        var mkinds   = props.getBoan().getMkinds();

        log.info("▶ Starting news fetch: {} static feeds + {} boan feeds",
                feeds.size(), mkinds.size());

        // 1) 고정 RSS
        feeds.forEach(this::fetchFromUrl);

        // 2) boannews: mkind 1~5
        mkinds.forEach(kind ->
                fetchFromUrl(boanBase + "?mkind=" + kind)
        );

        log.info("✔ Completed news fetch cycle");
    }

    /**
     * 주어진 RSS URL 에 요청을 보내고, 파싱한 엔트리를 Kafka 로 발행
     */
    private void fetchFromUrl(String url) {
        log.info("  • Fetching feed: {}", url);
        try (XmlReader reader = new XmlReader(new URL(url))) {
            SyndFeed feed = new SyndFeedInput().build(reader);

            var items = feed.getEntries().stream()
                    .filter(distinctByKey(e -> e.getTitle() + "::" + e.getLink()))
                    .map(this::toDto)
                    .peek(dto -> dto.setSource("NEWS"))
                    .collect(Collectors.toList());

            items.forEach(dto ->
                    kafkaTemplate.send(KafkaProducerConfig.topicRawContent(), dto)
            );
            log.info("    ✓ Published {} items", items.size());

        } catch (Exception ex) {
            log.error("    ✘ Failed to fetch RSS: {}", url, ex);
        }
    }

    /**
     * SyndEntry → ContentMessageDto 변환
     */
    private ContentMessageDto toDto(SyndEntry entry) {
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
