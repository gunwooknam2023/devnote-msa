package com.example.devnote.stats_service.service;

import com.example.devnote.stats_service.dto.*;
import com.example.devnote.stats_service.dto.internal.RankedChannelIdDto;
import com.example.devnote.stats_service.dto.internal.RankedContentIdDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 랭킹 데이터를 다른 서비스로부터 조합하여 제공하는 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RankingService {

    private final WebClient.Builder webClientBuilder;

    /**
     * 가장 많이 찜한 콘텐츠 TOP 50 조회
     */
    public PageResponseDto<RankedContentDto> getTopFavoritedContents(int page, int size) {
        // 1. user-service에서 찜 많은 순서대로 contentId 목록과 찜 수를 가져옴
        RestPageResponse<RankedContentIdDto> pagedIds = webClientBuilder.baseUrl("http://user-service").build()
                .get()
                .uri("/internal/ranking/content-favorites?page={page}&size={size}", page, size)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<RestPageResponse<RankedContentIdDto>>() {})
                .block();

        if (pagedIds == null || pagedIds.getContent().isEmpty()) {
            return PageResponseDto.<RankedContentDto>builder().items(List.of()).page(page).size(size).build();
        }

        // 2. 각 contentId로 processor-service에 상세 정보를 요청
        AtomicLong rankCounter = new AtomicLong(pagedIds.getNumber() * pagedIds.getSize() + 1);
        List<RankedContentDto> rankedItems = pagedIds.getContent().stream()
                .map(rankedId -> {
                    ApiResponseDto<ContentDto> details = fetchContentDetailsBlocking(rankedId.getContentId());
                    return toRankedContentDto(rankedId, details, rankCounter.getAndIncrement());
                })
                .collect(Collectors.toList());

        return PageResponseDto.<RankedContentDto>builder()
                .items(rankedItems)
                .page(pagedIds.getNumber())
                .size(pagedIds.getSize())
                .totalElements(pagedIds.getTotalElements())
                .totalPages(pagedIds.getTotalPages())
                .build();
    }

    /**
     * 가장 댓글이 많은 콘텐츠 TOP 50 조회
     */
    public PageResponseDto<RankedContentDto> getTopCommentedContents(int page, int size) {
        RestPageResponse<RankedContentIdDto> pagedIds = webClientBuilder.baseUrl("http://user-service").build()
                .get()
                .uri("/internal/ranking/content-comments?page={page}&size={size}", page, size)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<RestPageResponse<RankedContentIdDto>>() {})
                .block();

        if (pagedIds == null || pagedIds.getContent().isEmpty()) {
            return PageResponseDto.<RankedContentDto>builder().items(List.of()).page(page).size(size).build();
        }

        AtomicLong rankCounter = new AtomicLong(pagedIds.getNumber() * pagedIds.getSize() + 1);
        List<RankedContentDto> rankedItems = pagedIds.getContent().stream()
                .map(rankedId -> {
                    ApiResponseDto<ContentDto> details = fetchContentDetailsBlocking(rankedId.getContentId());
                    return toRankedContentDto(rankedId, details, rankCounter.getAndIncrement());
                })
                .collect(Collectors.toList());

        return PageResponseDto.<RankedContentDto>builder()
                .items(rankedItems)
                .page(pagedIds.getNumber())
                .size(pagedIds.getSize())
                .totalElements(pagedIds.getTotalElements())
                .totalPages(pagedIds.getTotalPages())
                .build();
    }

    /**
     * 가장 많이 찜한 채널 TOP 10 조회
     */
    public List<RankedChannelDto> getTopFavoritedChannels() {
        List<RankedChannelIdDto> rankedIds = webClientBuilder.baseUrl("http://user-service").build()
                .get()
                .uri("/internal/ranking/channel-favorites")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RankedChannelIdDto>>() {})
                .block();

        if (rankedIds == null || rankedIds.isEmpty()) {
            return List.of();
        }

        AtomicLong rankCounter = new AtomicLong(1);
        return rankedIds.stream()
                .map(rankedId -> {
                    ApiResponseDto<ChannelSubscriptionDto> details = fetchChannelDetailsBlocking(rankedId.getChannelId());
                    return toRankedChannelDto(rankedId, details, rankCounter.getAndIncrement());
                })
                .collect(Collectors.toList());
    }

    /**
     * 활동 우수 사용자 TOP 10 조회 (순차 처리)
     */
    public List<com.example.devnote.stats_service.dto.RankedUserDto> getTopActiveUsers() {
        // 1. user-service에서 활동 우수 사용자 목록을 가져옴
        List<RankedUserDto> users = webClientBuilder.baseUrl("http://user-service").build()
                .get()
                .uri("/internal/ranking/active-users")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RankedUserDto>>() {})
                .block();

        if (users == null || users.isEmpty()) {
            return List.of();
        }

        // 2. 순위를 매겨서 최종 DTO로 변환
        AtomicLong rankCounter = new AtomicLong(1);
        return users.stream()
                .map(user -> toRankedUserDto(user, rankCounter.getAndIncrement()))
                .collect(Collectors.toList());
    }

    /**
     * (Helper) 최종 응답 DTO로 변환
     */
    private com.example.devnote.stats_service.dto.RankedUserDto toRankedUserDto(RankedUserDto internalDto, long rank) {
        return com.example.devnote.stats_service.dto.RankedUserDto.builder()
                .rank(rank)
                .id(internalDto.getId())
                .name(internalDto.getName())
                .picture(internalDto.getPicture())
                .activityScore(internalDto.getActivityScore())
                .build();
    }

    /**
     * (Helper) contentId로 상세 정보 조회 (블로킹 방식)
     */
    private ApiResponseDto<ContentDto> fetchContentDetailsBlocking(Long contentId) {
        try {
            return webClientBuilder.baseUrl("http://processor-service").build()
                    .get()
                    .uri("/api/v1/contents/{id}", contentId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch content details for id={}: {}", contentId, e.getMessage());
            return new ApiResponseDto<>();
        }
    }

    /**
     * (Helper) channelId로 상세 정보 조회 (블로킹 방식)
     */
    private ApiResponseDto<ChannelSubscriptionDto> fetchChannelDetailsBlocking(Long channelId) {
        try {
            return webClientBuilder.baseUrl("http://news-youtube-service").build()
                    .get()
                    .uri("/api/v1/channels/{id}", channelId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ChannelSubscriptionDto>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch channel details for id={}: {}", channelId, e.getMessage());
            return new ApiResponseDto<>();
        }
    }

    private RankedContentDto toRankedContentDto(RankedContentIdDto id, ApiResponseDto<ContentDto> details, long rank) {
        ContentDto data = details.getData();
        if (data == null) {
            return RankedContentDto.builder()
                    .rank(rank)
                    .count(id.getCount())
                    .id(id.getContentId())
                    .title("콘텐츠 정보를 찾을 수 없습니다.")
                    .build();
        }
        return RankedContentDto.builder()
                .rank(rank)
                .count(id.getCount())
                .id(data.getId())
                .source(data.getSource())
                .category(data.getCategory())
                .title(data.getTitle())
                .link(data.getLink())
                .thumbnailUrl(data.getThumbnailUrl())
                .publishedAt(data.getPublishedAt())
                .channelTitle(data.getChannelTitle())
                .channelId(data.getChannelId())
                .viewCount(data.getViewCount())
                .description(data.getDescription())
                .createdAt(data.getCreatedAt())
                .channelThumbnailUrl(data.getChannelThumbnailUrl())
                .localViewCount(data.getLocalViewCount())
                .durationSeconds(data.getDurationSeconds())
                .videoForm(data.getVideoForm())
                .subscriberCount(data.getSubscriberCount())
                .build();
    }

    private RankedChannelDto toRankedChannelDto(RankedChannelIdDto id, ApiResponseDto<ChannelSubscriptionDto> details, long rank) {
        ChannelSubscriptionDto data = details.getData();
        if (data == null) {
            return RankedChannelDto.builder()
                    .rank(rank)
                    .count(id.getCount())
                    .id(id.getChannelId())
                    .youtubeName("채널 정보를 찾을 수 없습니다.")
                    .build();
        }
        return RankedChannelDto.builder()
                .rank(rank)
                .count(id.getCount())
                .id(data.getId())
                .youtubeName(data.getYoutubeName())
                .channelId(data.getChannelId())
                .channelThumbnailUrl(data.getChannelThumbnailUrl())
                .subscriberCount(data.getSubscriberCount())
                .build();
    }

    @lombok.Data
    private static class RestPageResponse<T> {
        private List<T> content;
        private int number;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}