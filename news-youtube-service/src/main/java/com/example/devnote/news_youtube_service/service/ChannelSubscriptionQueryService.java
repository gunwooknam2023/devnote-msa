package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.dto.ChannelSubscriptionSummaryDto;
import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import com.example.devnote.news_youtube_service.repository.ChannelSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전체 채널 구독 현황 조회 로직
 */
@Service
@RequiredArgsConstructor
public class ChannelSubscriptionQueryService {

    private final ChannelSubscriptionRepository channelSubscriptionRepository;

    /**
     * 소스별(유튜브/뉴스) 채널 리스트를 DTO로 변환
     */
    public List<ChannelSubscriptionSummaryDto> getSubscriptionsBySource(String source) {
        return channelSubscriptionRepository.findBySource(source).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    private ChannelSubscriptionSummaryDto toSummaryDto(ChannelSubscription entity) {
        return ChannelSubscriptionSummaryDto.builder()
                .channelThumbnail(entity.getChannelThumbnailUrl())
                .channelName(entity.getYoutubeName())
                .source(entity.getSource())
                .subscriberCount(entity.getSubscriberCount())
                .favoriteCount(entity.getFavoriteCount())
                .build();
    }
}
