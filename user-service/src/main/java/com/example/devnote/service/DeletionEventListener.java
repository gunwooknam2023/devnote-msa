package com.example.devnote.service;

import com.example.devnote.repository.FavoriteChannelRepository;
import com.example.devnote.repository.FavoriteContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeletionEventListener {
    private final FavoriteChannelRepository channelFavRepo;
    private final FavoriteContentRepository contentFavRepo;

    /**
     * channel.deleted 토픽 수신 → 해당 채널 즐겨찾기 삭제
     */
    @KafkaListener(
            topics = "channel.deleted",
            containerFactory = "deletionKafkaListenerFactory"
    )
    @Transactional
    public void onChannelDeleted(String channelIdStr) {
        Long channelId = Long.parseLong(channelIdStr);
        channelFavRepo.deleteByChannelSubscriptionId(channelId);
        log.info("Deleted {} channel favorites", channelId);
    }

    /**
     * content.deleted 토픽 수신 → 해당 콘텐츠 즐겨찾기 삭제
     */
    @KafkaListener(
            topics = "content.deleted",
            containerFactory = "deletionKafkaListenerFactory"
    )
    @Transactional
    public void onContentDeleted(String contentIdStr) {
        Long contentId = Long.parseLong(contentIdStr);
        contentFavRepo.deleteByContentId(contentId);
        log.info("Deleted {} content favorites", contentId);
    }
}
