package com.example.devnote.processor_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "classification")
public class ClassificationProperties {
    private Scheme youtube;
    private Scheme news;

    // Getter, Setter
    public Scheme getYoutube() { return youtube; }
    public void setYoutube(Scheme youtube) { this.youtube = youtube; }
    public Scheme getNews() { return news; }
    public void setNews(Scheme news) { this.news = news; }

    /**
     * 각 분류 체계(Scheme)의 설정을 담는 내부 클래스
     */
    public static class Scheme {
        private List<String> labels;
        private String systemPrompt;
        private String userPromptTemplate;

        public List<String> getLabels() {
            return labels;
        }
        public void setLabels(List<String> labels) {
            this.labels = labels;
        }
        public String getSystemPrompt() {
            return systemPrompt;
        }
        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }
        public String getUserPromptTemplate() {
            return userPromptTemplate;
        }
        public void setUserPromptTemplate(String userPromptTemplate) {
            this.userPromptTemplate = userPromptTemplate;
        }
    }
}