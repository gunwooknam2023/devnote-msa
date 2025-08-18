package com.example.devnote.service;

import com.example.devnote.dto.RankedChannelIdDto;
import com.example.devnote.dto.RankedContentIdDto;
import com.example.devnote.dto.RankedUserDto;
import com.example.devnote.entity.User;
import com.example.devnote.repository.CommentRepository;
import com.example.devnote.repository.FavoriteChannelRepository;
import com.example.devnote.repository.FavoriteContentRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 랭킹 데이터 집계 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final FavoriteContentRepository favoriteContentRepository;
    private final CommentRepository commentRepository;
    private final FavoriteChannelRepository favoriteChannelRepository;
    private final UserRepository userRepository;

    /**
     * 가장 많이 찜한 콘텐츠 랭킹 조회
     */
    public Page<RankedContentIdDto> getTopFavoritedContents(Pageable pageable) {
        return favoriteContentRepository.findTopFavoritedContentIds(pageable);
    }

    /**
     * 가장 댓글이 많은 콘텐츠 랭킹 조회
     */
    public Page<RankedContentIdDto> getTopCommentedContents(Pageable pageable) {
        return commentRepository.findTopCommentedContentIds(pageable);
    }

    /**
     * 가장 많이 찜한 채널 랭킹 조회
     */
    public List<RankedChannelIdDto> getTopFavoritedChannels(Pageable pageable) {
        return favoriteChannelRepository.findTopFavoritedChannelIds(pageable);
    }

    /**
     * 활동 점수가 높은 사용자 TOP 10 조회
     */
    public List<RankedUserDto> getTopActiveUsers() {
        return userRepository.findTop10ByOrderByActivityScoreDesc().stream()
                .map(this::toRankedUserDto)
                .collect(Collectors.toList());
    }

    /**
     * User Entity -> RankedUserDto 변환 헬퍼 메서드
     */
    private RankedUserDto toRankedUserDto(User user) {
        return RankedUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .picture(user.getPicture())
                .activityScore(user.getActivityScore())
                .build();
    }
}