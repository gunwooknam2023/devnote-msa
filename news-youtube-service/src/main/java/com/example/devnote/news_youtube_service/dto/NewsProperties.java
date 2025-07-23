package com.example.devnote.news_youtube_service.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "news")
public class NewsProperties {
    private List<String> feeds; // 고정 RSS URL 목록
    private Boan boan; // boannews 관련 설정
    private long fetchDelay; // 뉴스 fetch 스케줄 딜레이 (ms)


    public List<String> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<String> feeds) {
        this.feeds = feeds;
    }

    public Boan getBoan() {
        return boan;
    }

    public void setBoan(Boan boan) {
        this.boan = boan;
    }

    public long getFetchDelay() {
        return fetchDelay;
    }

    public void setFetchDelay(long fetchDelay) {
        this.fetchDelay = fetchDelay;
    }

    public static class Boan {
        private String baseUrl; // boannews base-url
        private List<Integer> mkinds; // mkind 리스트

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public List<Integer> getMkinds() {
            return mkinds;
        }

        public void setMkinds(List<Integer> mkinds) {
            this.mkinds = mkinds;
        }
    }
}