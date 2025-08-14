package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.config.ClassificationProperties;
import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.repository.ContentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 기반 카테고리 자동 분류 (최대 50개씩 미니 배치 처리 및 즉시 저장)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryClassificationService {
    private final ContentRepository contentRepository;
    private final ClassificationProperties props;
    private final Client genaiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 한 번의 API 호출에 보낼 콘텐츠의 최대 개수 */
    private static final int BATCH_SIZE = 50;

    /**
     * 1시간마다 카테고리가 'TBC'인 모든 콘텐츠를 조회하여 50개씩 묶어 일괄 분류 후 즉시 업데이트
     */
    @Scheduled(fixedDelayString = "3600000")
    public void classifyTbcContents() {
        List<ContentEntity> toClassify = contentRepository.findBySourceAndCategory("YOUTUBE", "TBC");
        if (toClassify.isEmpty()) {
            log.info("[AI-CLASSIFY] No content to classify.");
            return;
        }
        log.info("[AI-CLASSIFY] Found {} contents to classify. Processing in batches of {}.", toClassify.size(), BATCH_SIZE);

        int totalBatches = (int) Math.ceil((double) toClassify.size() / BATCH_SIZE);

        for (int i = 0; i < toClassify.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toClassify.size());
            List<ContentEntity> batch = toClassify.subList(i, end);
            int currentBatchNum = (i / BATCH_SIZE) + 1;
            log.info("[AI-CLASSIFY] Processing batch {}/{} ({} items)", currentBatchNum, totalBatches, batch.size());

            try {
                String prompt = buildBatchPrompt(batch);

                GenerateContentResponse resp = genaiClient.models
                        .generateContent("gemini-2.5-flash", prompt, null);

                String responseText = resp.text();

                Map<Long, String> classifiedCategories = parseAiResponse(responseText);

                List<ContentEntity> updatedInBatch = new ArrayList<>();
                for (ContentEntity entity : batch) {
                    String category = classifiedCategories.get(entity.getId());
                    if (category != null && props.getLabels().contains(category)) {
                        entity.setCategory(category);
                        updatedInBatch.add(entity);
                    }
                }

                // 현재 배치에서 성공적으로 업데이트된 엔티티들을 즉시 DB에 저장
                if (!updatedInBatch.isEmpty()) {
                    contentRepository.saveAll(updatedInBatch);
                    log.info("[AI-CLASSIFY] Batch {}/{} finished. Successfully updated {} contents in DB.", currentBatchNum, totalBatches, updatedInBatch.size());
                } else {
                    log.info("[AI-CLASSIFY] Batch {}/{} finished. No items were updated.", currentBatchNum, totalBatches);
                }
                // ===============================================================

            } catch (Exception ex) {
                log.error("[AI-CLASSIFY] Failed to process batch {}/{}.", currentBatchNum, totalBatches, ex);
            }
        }
        log.info("[AI-CLASSIFY] All batch processing finished.");
    }

    /**
     * 여러 콘텐츠 정보를 바탕으로 AI에 전달할 전체 프롬프트를 생성
     */
    private String buildBatchPrompt(List<ContentEntity> entities) {
        String titlesJson = entities.stream()
                .map(e -> String.format("{\"id\": %d, \"title\": \"%s\"}", e.getId(), escapeJson(e.getTitle())))
                .collect(Collectors.joining(",\n", "[\n", "\n]"));

        String system = props.getSystemPrompt();
        String userTemplate = props.getUserPromptTemplate();
        String labels = String.join(", ", props.getLabels());

        return system + "\n\n" + String.format(userTemplate, labels, titlesJson);
    }

    /**
     * AI가 반환한 JSON 문자열 응답 파싱
     */
    private Map<Long, String> parseAiResponse(String jsonResponse) throws Exception {
        String cleanedJson = jsonResponse.trim().replace("```json", "").replace("```", "").trim();
        List<Map<String, Object>> results = objectMapper.readValue(cleanedJson, new TypeReference<>() {});

        return results.stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("id")).longValue(),
                        map -> (String) map.get("category")
                ));
    }

    /**
     * JSON 문자열에 포함될 수 있는 특수문자를 이스케이프 처리
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}