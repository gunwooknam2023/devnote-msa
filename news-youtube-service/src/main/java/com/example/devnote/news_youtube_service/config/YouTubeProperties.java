package com.example.devnote.news_youtube_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "youtube")
public class YouTubeProperties {

    private List<Channel> channels;

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    /**
     * 각 유튜브 채널의 설정을 담는 내부 정적 클래스
     */
    public static class Channel {
        private String name;
        private String channelId;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getChannelId() {
            return channelId;
        }
        public void setChannelId(String channelId) {
            this.channelId = channelId;
        }
    }
}