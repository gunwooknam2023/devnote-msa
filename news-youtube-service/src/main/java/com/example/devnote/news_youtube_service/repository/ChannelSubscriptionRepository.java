package com.example.devnote.news_youtube_service.repository;

import com.example.devnote.news_youtube_service.entity.ChannelSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelSubscriptionRepository extends JpaRepository<ChannelSubscription, Long> {
    List<ChannelSubscription> findByInitialLoadedFalse();
    List<ChannelSubscription> findByInitialLoadedTrue();
}
