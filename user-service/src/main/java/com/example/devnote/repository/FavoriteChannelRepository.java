package com.example.devnote.repository;

import com.example.devnote.dto.RankedChannelIdDto;
import com.example.devnote.entity.FavoriteChannel;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteChannelRepository extends JpaRepository<FavoriteChannel, Long> {
    Optional<FavoriteChannel> findByUserIdAndChannelSubscriptionId(Long userId, Long chSubId);
    List<FavoriteChannel> findByUserId(Long userId);
    List<FavoriteChannel> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByChannelSubscriptionId(Long channelSubscriptionId);
    /**
     * 특정 source에서 가장 많이 찜한 채널 ID와 찜 수를 TOP 10 조회
     */
    @Query("SELECT new com.example.devnote.dto.RankedChannelIdDto(fc.channelSubscriptionId, COUNT(fc.id)) " +
            "FROM FavoriteChannel fc " +
            "WHERE fc.source = :source " +
            "GROUP BY fc.channelSubscriptionId " +
            "ORDER BY COUNT(fc.id) DESC, fc.channelSubscriptionId ASC")
    List<RankedChannelIdDto> findTop10FavoritedChannelIdsBySource(@Param("source") String source, Pageable pageable);

    /**
     * 특정 사용자가 찜한 채널/언론사의 총 개수를 조회
     */
    long countByUserId(Long userId);

    /**
     * 특정 사용자의 모든 채널/언론사 찜 기록을 삭제
     */
    @Modifying
    void deleteAllByUserId(Long userId);
}
