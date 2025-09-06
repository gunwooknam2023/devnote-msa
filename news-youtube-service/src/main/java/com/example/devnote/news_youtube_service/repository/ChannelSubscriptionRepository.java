package com.example.devnote.news_youtube_service.repository;

import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelSubscriptionRepository extends JpaRepository<ChannelSubscription, Long> {
    Optional<ChannelSubscription> findByChannelId(String channelId);
    List<ChannelSubscription> findBySource(String source);
    List<ChannelSubscription> findBySourceAndInitialLoadedFalse(String source);
    List<ChannelSubscription> findBySourceAndInitialLoadedTrue(String source);
}
