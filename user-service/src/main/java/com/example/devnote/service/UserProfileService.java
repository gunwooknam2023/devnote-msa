package com.example.devnote.service;

import com.example.devnote.dto.*;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.FavoriteContent;
import com.example.devnote.entity.User;
import com.example.devnote.entity.WithdrawnUser;
import com.example.devnote.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    private final UserRepository userRepo;
    private final FavoriteContentRepository favContentRepo;
    private final FavoriteChannelRepository favChannelRepo;
    private final CommentRepository commentRepo;
    private final WebClient apiGatewayClient;
    private final WithdrawnUserRepository withdrawnUserRepo;
    private final CommentService commentService;
    private final ContentFavoriteService contentFavoriteService;
    private final ChannelFavoriteService channelFavoriteService;


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

    /** 현재 로그인된 사용자의 회원 탈퇴를 처리. 모든 활동 기록을 삭제하고, 재가입 방지를 위한 정보를 남긴 뒤, 회원 정보를 최종 삭제합니다. */
    @Transactional
    public void withdrawCurrentUser() {
        // 1. 현재 로그인한 사용자 정보 조회
        User user = currentUser();
        Long userId = user.getId();
        log.info("회원 탈퇴를 시작합니다: userId={}, email={}", userId, user.getEmail());

        // 2. 재가입 방지를 위해 탈퇴 정보 테이블에 기록
        WithdrawnUser withdrawnUser = WithdrawnUser.builder()
                .email(user.getEmail())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .canRejoinAt(Instant.now().plus(7, ChronoUnit.DAYS)) // 7일 후 재가입 가능
                .build();
        withdrawnUserRepo.save(withdrawnUser);
        log.info("탈퇴 정보 기록 완료. 재가입 방지 기간이 설정되었습니다.");

        // 3. 사용자의 모든 활동 기록 삭제 (Kafka 이벤트 발행을 위해 각 서비스 메서드 호출)
        // 3-1. 찜한 콘텐츠 일괄 삭제
        List<FavoriteContent> favoriteContents = favContentRepo.findByUserId(userId);
        log.info("찜한 콘텐츠 {}건에 대한 삭제 처리를 시작합니다.", favoriteContents.size());
        favoriteContents.forEach(fav -> contentFavoriteService.remove(fav.getContentId()));

        // 3-2. 찜한 채널/언론사 일괄 삭제
        log.info("찜한 채널/언론사 {}건에 대한 삭제 처리를 시작합니다.", favChannelRepo.countByUserId(userId));
        favChannelRepo.deleteAllByUserId(userId); // 채널 찜은 별도 이벤트가 없으므로 직접 삭제

        // 3-3. 작성한 댓글 일괄 삭제
        List<CommentEntity> comments = commentRepo.findByUserIdOrderByCreatedAtDesc(userId);
        log.info("작성 댓글 {}건에 대한 삭제 처리를 시작합니다.", comments.size());
        comments.forEach(comment -> {
            // 회원 댓글이므로 비밀번호 없이 삭제 (서비스 내부 로직에 의해 권한 통과)
            commentService.deleteComment(comment.getId(), null);
        });

        // 4. 모든 활동 기록 삭제 후, 최종적으로 User 엔티티 삭제
        userRepo.delete(user);
        log.info("회원 정보 최종 삭제 완료: userId={}", userId);
    }

    /** CommentEntity -> CommentResponseDto 변환 */
    private CommentResponseDto toCommentDto(CommentEntity e) {
        List<CommentResponseDto> replies = commentRepo.findByParentIdOrderByCreatedAtAsc(e.getId())
                .stream()
                .map(this::toCommentDto)
                .toList();

        // 2) 콘텐츠 정보 조회 (제목 + 소스)
        String contentTitle = "[삭제된 콘텐츠]";
        String contentSource = "-";
        String contentLink = "#";

        try {
            // 콘텐츠 정보 조회 (제목 + 소스)
            ApiResponseDto<ContentDto> cr = apiGatewayClient.get()
                    .uri("/api/v1/contents/{id}", e.getContentId())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                    .block();

            if (cr != null && cr.getData() != null) {
                contentTitle = cr.getData().getTitle();
                contentSource = cr.getData().getSource();
                contentLink = cr.getData().getLink();
            }
        } catch (WebClientResponseException.NotFound ex) {
            // 콘텐츠를 찾지 못하면(404) 로그만 남기고 기본값으로 넘어감
            log.warn("Content not found for commentId {}: contentId={}. Skipping details.", e.getId(), e.getContentId());
        }

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
