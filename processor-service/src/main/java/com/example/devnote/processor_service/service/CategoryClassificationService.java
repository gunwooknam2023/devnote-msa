package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.config.ClassificationProperties;
import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.repository.ContentRepository;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 기반 카테고리 자동 분류
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryClassificationService {
    private final ContentRepository contentRepository;
    private final ClassificationProperties props;
    private final Client genaiClient;

    /**
     * 1시간마다 YOUTUBE 영상목록중 카테고리가 TBC 인것을 조회해서 AI로 분류 후 업데이트
     */
    @Scheduled(fixedDelayString = "3600000")
    public void classifyTbcContents() {
        List<ContentEntity> toClassify = contentRepository
                .findBySourceAndCategory("YOUTUBE", "TBC");

        toClassify.forEach(ent -> {
            try {
                String label = callGemini(ent.getTitle());
                ent.setCategory(label);
                contentRepository.save(ent);
                log.info("Classified id={} title='{}' → {}", ent.getId(), ent.getTitle(), label);
            } catch (Exception ex) {
                log.warn("Failed to classify id={} title='{}'", ent.getId(), ent.getTitle(), ex);
            }
        });
    }

    /**
     * Gemini 2.5 Flash 호출 후 카테고리 받아오기
     */
    private String callGemini(String title) {
        String system = props.getSystemPrompt();
        String user   = String.format(
                props.getUserPromptTemplate(),
                String.join(", ", props.getLabels()),
                title
        );

        // 프롬프트 결합
        String prompt = system + "\n\n" + user;

        // generateContent 호출
        GenerateContentResponse resp = genaiClient.models
                .generateContent("gemini-2.5-flash", prompt, null);

        // 결과 가져오기 + 공백 제거
        String aiLabel = resp.text().trim();

        // 유효성 검사: 없으면 "TBC"로 폴백
        return props.getLabels().contains(aiLabel) ? aiLabel : "TBC";
    }
}
