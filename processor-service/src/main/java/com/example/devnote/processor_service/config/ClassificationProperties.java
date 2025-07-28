package com.example.devnote.processor_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "classification")
public class ClassificationProperties {
    private List<String> labels; // 카테고리
    private String systemPrompt; // 역할 부여
    private String userPromptTemplate; // 메시지 템플릿

    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getUserPromptTemplate() { return userPromptTemplate; }
    public void setUserPromptTemplate(String userPromptTemplate) { this.userPromptTemplate = userPromptTemplate; }
}