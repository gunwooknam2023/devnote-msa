package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 대시보드
 * 찜한 영상 목록
 * 찜한 뉴스 목록
 * 찜한 채널 목록
 * 작성한 댓글 목록
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {
    /** 찜한 YouTube 영상들 */
    private Page<ContentDto> favoriteVideos;

    /** 찜한 News 콘텐츠들 */
    private Page<ContentDto> favoriteNews;

    /** 찜한 채널들 */
    private Page<ChannelSubscriptionDto> favoriteChannels;

    /** 작성한 댓글들 */
    private Page<CommentResponseDto> comments;

    /** 활동 점수 */
    private Integer activityScore;
}
