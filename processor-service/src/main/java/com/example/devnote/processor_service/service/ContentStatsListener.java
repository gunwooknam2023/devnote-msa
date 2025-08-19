package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.dto.ContentStatsUpdateDto;
import com.example.devnote.processor_service.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentStatsListener {

    private final ContentRepository contentRepository;

    /**
     * 'content-stats-update' 토픽을 구독하여 찜/댓글 수를 DB에 반영
     */
    @KafkaListener(
            topics = "content-stats-update",
            containerFactory = "statsUpdateContainerFactory"
    )
    @Transactional
    public void listen(ContentStatsUpdateDto message) {
        log.info("Received stats update message: {}", message);
        contentRepository.findById(message.getContentId()).ifPresent(content -> {
            // 현재 값에 변화량(delta)을 더하여 업데이트
            content.setFavoriteCount(content.getFavoriteCount() + message.getFavoriteDelta());
            content.setCommentCount(content.getCommentCount() + message.getCommentDelta());
            // 변경된 내용 저장
            contentRepository.save(content);
        });
    }
}
