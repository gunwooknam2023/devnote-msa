package com.example.devnote.service;

import com.example.devnote.dto.*;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.FavoriteContent;
import com.example.devnote.entity.User;
import com.example.devnote.repository.CommentRepository;
import com.example.devnote.repository.FavoriteChannelRepository;
import com.example.devnote.repository.FavoriteContentRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepo;
    private final FavoriteContentRepository favContentRepo;
    private final FavoriteChannelRepository favChannelRepo;
    private final CommentRepository commentRepo;
    private final WebClient apiGatewayClient;


    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /** 특정 사용자의 프로필 정보 조회 */
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 찜한 영상 개수
        int favoriteVideoCount = favContentRepo.findByUserId(userId).stream()
                .mapToInt(fav -> {
                    try {
                        ApiResponseDto<ContentDto> response = apiGatewayClient.get()
                                .uri("/api/v1/contents/{id}", fav.getContentId())
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                                .block();
                        return "YOUTUBE".equals(response.getData().getSource()) ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        // 찜한 뉴스 개수
        int favoriteNewsCount = favContentRepo.findByUserId(userId).stream()
                .mapToInt(fav -> {
                    try {
                        ApiResponseDto<ContentDto> response = apiGatewayClient.get()
                                .uri("/api/v1/contents/{id}", fav.getContentId())
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                                .block();
                        return "NEWS".equals(response.getData().getSource()) ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        // 찜한 채널 개수
        int favoriteChannelCount = favChannelRepo.findByUserId(userId).size();

        // 작성한 댓글 개수 (대댓글 포함)
        int commentCount = commentRepo.countTotalCommentsByUserId(userId);

        return UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .picture(user.getPicture())
                .selfIntroduction(user.getSelfIntroduction())
                .activityScore(user.getActivityScore())
                .favoriteVideoCount(favoriteVideoCount)
                .favoriteNewsCount(favoriteNewsCount)
                .favoriteChannelCount(favoriteChannelCount)
                .commentCount(commentCount)
                .build();
    }

    /** 자기소개 수정 */
    @Transactional
    public String update(ProfileRequestDto req) {
        User me = currentUser();
        me.setSelfIntroduction(req.getSelfIntroduction());
        userRepo.save(me);
        return me.getSelfIntroduction();
    }

    /** 프로필 페이지 정보 반환 (찜 영상, 뉴스, 채널 / 작성한 댓글 목록) */
    public DashboardDto getDashboard() {
        User currentUser = currentUser();
        Long userId = currentUser.getId();

        // 1) 찜한 콘텐츠 ID 목록
        List<Long> contentIds = favContentRepo.findByUserId(userId)
                .stream()
                .map(FavoriteContent::getContentId)
                .toList();

        // 2) API-GateWay 호출 -> ContentDto 로 변환
        List<ContentDto> allFavorites = contentIds.stream()
                .map(id -> apiGatewayClient
                        .get()
                        .uri("/api/v1/contents/{id}", id)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {
                        })
                        .block()
                        .getData()
                ).toList();

        // 3) source 별로 분리
        List<ContentDto> favoriteVideos = allFavorites.stream()
                .filter(c -> "YOUTUBE".equals(c.getSource()))
                .toList();

        List<ContentDto> favoriteNews = allFavorites.stream()
                .filter(c -> "NEWS".equals(c.getSource()))
                .toList();

        // 4) 찜한 채널 ID -> ChannelSubscription 조회
        List<ChannelSubscriptionDto> favoriteChannels = favChannelRepo.findByUserId(userId)
                .stream()
                .map(fav -> apiGatewayClient
                        .get()
                        .uri("/api/v1/channels/{id}", fav.getChannelSubscriptionId())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ChannelSubscriptionDto>>() {
                        })
                        .block()
                        .getData()
                ).toList();

        // 5) 작성한 댓글
        List<CommentResponseDto> comments = commentRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toCommentDto)
                .toList();

        return DashboardDto.builder()
                .favoriteVideos(favoriteVideos)
                .favoriteNews(favoriteNews)
                .favoriteChannels(favoriteChannels)
                .comments(comments)
                .activityScore(currentUser.getActivityScore())
                .build();
    }

    /** CommentEntity -> CommentResponseDto 변환 */
    private CommentResponseDto toCommentDto(CommentEntity e) {
        List<CommentResponseDto> replies = commentRepo.findByParentIdOrderByCreatedAtAsc(e.getId())
                .stream()
                .map(this::toCommentDto)
                .toList();

        // 2) 콘텐츠 정보 조회 (제목 + 소스)
        ApiResponseDto<ContentDto> cr = apiGatewayClient.get()
                .uri("/api/v1/contents/{id}", e.getContentId())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {
                })
                .block();
        String contentTitle = cr.getData().getTitle();
        String contentSource = cr.getData().getSource();
        String contentLink   = cr.getData().getLink();

        // userId가 null(익명)일 수 있으므로 null-safe
        String upic = null;
        if (e.getUserId() != null) {
            upic = userRepo.findById(e.getUserId()).map(User::getPicture).orElse(null);
        }

        return CommentResponseDto.builder()
                .id(e.getId())
                .parentId(e.getParentId())
                .contentId(e.getContentId())
                .userId(e.getUserId())
                .username(e.getUsername())
                .userPicture(upic)
                .content(e.getContent())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .replies(replies)
                .contentTitle(contentTitle)
                .contentSource(contentSource)
                .contentLink(contentLink)
                .build();
    }
}
