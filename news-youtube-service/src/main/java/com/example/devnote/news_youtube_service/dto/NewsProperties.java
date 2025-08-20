package com.example.devnote.news_youtube_service.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "news")
public class NewsProperties {

    private long fetchDelay;
    private List<Source> sources;

    public long getFetchDelay() {
        return fetchDelay;
    }

    public void setFetchDelay(long fetchDelay) {
        this.fetchDelay = fetchDelay;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    /**
     * 각 언론사(Source)의 설정을 담는 내부 정적 클래스
     */
    public static class Source {
        private String name;
        private String thumbnailUrl;
        private List<String> feeds;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }
        public List<String> getFeeds() {
            return feeds;
        }
        public void setFeeds(List<String> feeds) {
            this.feeds = feeds;
        }
    }
}