package com.example.devnote.repository;

import com.example.devnote.entity.FavoriteChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteChannelRepository extends JpaRepository<FavoriteChannel, Long> {
    Optional<FavoriteChannel> findByUserIdAndChannelSubscriptionId(Long userId, Long chSubId);
    List<FavoriteChannel> findByUserId(Long userId);
    void deleteByChannelSubscriptionId(Long channelSubscriptionId);
}
