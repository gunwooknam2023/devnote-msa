package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.dto.ChannelStatsUpdateDto;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelStatsListener {

    private final ChannelSubscriptionRepository channelSubscriptionRepository;

    /**
     * 'channel-stats-update' 토픽을 구독하여 찜 수를 DB에 반영
     */
    @KafkaListener(
            topics = "channel-stats-update",
            containerFactory = "statsUpdateContainerFactory"
    )
    @Transactional
    public void listen(ChannelStatsUpdateDto message) {
        log.info("Received channel stats update message: {}", message);
        channelSubscriptionRepository.findById(message.getChannelSubscriptionId()).ifPresent(channel -> {
            // 현재 찜 수에 변화량(delta)을 더하여 업데이트
            long newCount = channel.getFavoriteCount() + message.getFavoriteDelta();
            channel.setFavoriteCount(Math.max(0, newCount)); // 0 미만으로 내려가지 않도록 보정

            // 변경된 내용 저장
            channelSubscriptionRepository.save(channel);
        });
    }
}