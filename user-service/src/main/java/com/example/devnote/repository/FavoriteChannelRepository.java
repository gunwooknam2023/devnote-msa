package com.example.devnote.repository;

import com.example.devnote.dto.RankedChannelIdDto;
import com.example.devnote.entity.FavoriteChannel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteChannelRepository extends JpaRepository<FavoriteChannel, Long> {
    Optional<FavoriteChannel> findByUserIdAndChannelSubscriptionId(Long userId, Long chSubId);
    List<FavoriteChannel> findByUserId(Long userId);
    void deleteByChannelSubscriptionId(Long channelSubscriptionId);
    /**
     * 가장 많이 찜한 채널 ID와 찜 수를 페이지네이션하여 조회
     */
    @Query("SELECT new com.example.devnote.dto.RankedChannelIdDto(fc.channelSubscriptionId, COUNT(fc.id)) " +
            "FROM FavoriteChannel fc " +
            "GROUP BY fc.channelSubscriptionId " +
            "ORDER BY COUNT(fc.id) DESC, fc.channelSubscriptionId ASC")
    List<RankedChannelIdDto> findTopFavoritedChannelIds(Pageable pageable);
}
