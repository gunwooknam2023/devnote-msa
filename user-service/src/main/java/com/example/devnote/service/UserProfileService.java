package com.example.devnote.service;

import com.example.devnote.config.JwtTokenProvider;
import com.example.devnote.dto.*;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.FavoriteContent;
import com.example.devnote.entity.User;
import com.example.devnote.entity.WithdrawnUser;
import com.example.devnote.entity.enums.CommentTargetType;
import com.example.devnote.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    private final UserRepository userRepo;
    private final FavoriteContentRepository favContentRepo;
    private final FavoriteChannelRepository favChannelRepo;
    private final CommentRepository commentRepo;
    private final PostRepository postRepository;
    private final WebClient apiGatewayClient;
    private final WithdrawnUserRepository withdrawnUserRepo;
    private final CommentService commentService;
    private final ContentFavoriteService contentFavoriteService;
    private final ChannelFavoriteService channelFavoriteService;
    private final JwtTokenProvider tokenProvider;
    private final FileStorageService fileStorageService;

    @Value("${app.default-profile-image-url}")
    private String defaultProfileImageUrl;


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

    /**
     * 프로필 페이지 정보 반환 (찜 영상, 뉴스, 채널 / 작성한 댓글 목록)
     */
    public DashboardDto getDashboard(Pageable pageable) {
        User currentUser = currentUser();
        Long userId = currentUser.getId();

        // 1. 찜한 콘텐츠 처리 (영상/뉴스)
        List<Long> contentIds = favContentRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FavoriteContent::getContentId)
                .toList();

        List<ContentDto> allFavorites = contentIds.stream()
                .map(this::fetchContentDetails)
                .filter(Objects::nonNull)
                .toList();

        List<ContentDto> favoriteVideosList = allFavorites.stream()
                .filter(c -> "YOUTUBE".equals(c.getSource()))
                .toList();
        List<ContentDto> favoriteNewsList = allFavorites.stream()
                .filter(c -> "NEWS".equals(c.getSource()))
                .toList();

        Page<ContentDto> favoriteVideosPage = toPage(favoriteVideosList, pageable);
        Page<ContentDto> favoriteNewsPage = toPage(favoriteNewsList, pageable);

        // 2. 찜한 채널 처리
        List<ChannelSubscriptionDto> favoriteChannelsList = favChannelRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(fav -> fetchChannelDetails(fav.getChannelSubscriptionId()))
                .filter(Objects::nonNull)
                .toList();
        Page<ChannelSubscriptionDto> favoriteChannelsPage = toPage(favoriteChannelsList, pageable);

        // 3. 작성한 댓글 처리 (DB에서 직접 페이지네이션)
        Page<CommentEntity> commentPage = commentRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        Page<CommentResponseDto> commentsPage = commentPage.map(this::toCommentDto);

        // 4. 최종 DashboardDto 조립
        return DashboardDto.builder()
                .favoriteVideos(favoriteVideosPage)
                .favoriteNews(favoriteNewsPage)
                .favoriteChannels(favoriteChannelsPage)
                .comments(commentsPage)
                .activityScore(currentUser.getActivityScore())
                .build();
    }

    /**
     * 현재 로그인된 사용자의 회원 탈퇴를 처리. 모든 활동 기록을 삭제하고, 재가입 방지를 위한 정보를 남긴 뒤, 회원 정보를 최종 삭제
     */
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

    /**
     * List를 Page 객체로 변환하는 유틸리티 메서드
     */
    private <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());

        if (start > list.size()) {
            return new PageImpl<>(List.of(), pageable, list.size());
        }

        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    /**
     * 찜한 콘텐츠의 상세 정보를 조회하는 메서드
     */
    private ContentDto fetchContentDetails(Long contentId) {
        try {
            ApiResponseDto<ContentDto> response = apiGatewayClient.get()
                    .uri("/api/v1/contents/{id}", contentId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                    .block();
            return (response != null) ? response.getData() : null;
        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Favorite content not found (id={}), skipping.", contentId);
            return null;
        }
    }

    /**
     * 찜한 채널의 상세 정보를 조회하는 메서드
     */
    private ChannelSubscriptionDto fetchChannelDetails(Long channelId) {
        try {
            ApiResponseDto<ChannelSubscriptionDto> response = apiGatewayClient.get()
                    .uri("/api/v1/channels/{id}", channelId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ChannelSubscriptionDto>>() {})
                    .block();
            return (response != null) ? response.getData() : null;
        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Favorite channel not found (id={}), skipping.", channelId);
            return null;
        }
    }


    /** CommentEntity -> CommentResponseDto 변환 */
    private CommentResponseDto toCommentDto(CommentEntity e) {
        List<CommentResponseDto> replies = commentRepo.findByParentIdOrderByCreatedAtAsc(e.getId())
                .stream()
                .map(this::toCommentDto)
                .toList();

        // 람다 내에서 변경 가능한 final 배열 사용
        final String[] targetInfo = {"[삭제됨]", "-", "#"}; // {title, source, link}

        if (e.getTargetType() == CommentTargetType.CONTENT) {
            try {
                ApiResponseDto<ContentDto> cr = apiGatewayClient.get()
                        .uri("/api/v1/contents/{id}", e.getTargetId())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                        .block();

                if (cr != null && cr.getData() != null) {
                    targetInfo[0] = cr.getData().getTitle();
                    targetInfo[1] = cr.getData().getSource();
                    targetInfo[2] = cr.getData().getLink();
                }
            } catch (WebClientResponseException.NotFound ex) {
                log.warn("Content not found for commentId {}: targetId={}.", e.getId(), e.getTargetId());
            }
        } else if (e.getTargetType() == CommentTargetType.POST) {
            postRepository.findById(e.getTargetId()).ifPresentOrElse(
                    post -> {
                        targetInfo[0] = post.getTitle();
                        targetInfo[1] = post.getBoardType().name();
                        targetInfo[2] = "/posts/" + post.getId();
                    },
                    () -> log.warn("Post not found for commentId {}: targetId={}.", e.getId(), e.getTargetId())
            );
        }

        // userId가 null(익명)일 수 있으므로 null-safe
        String upic = null;
        if (e.getUserId() != null) {
            upic = userRepo.findById(e.getUserId()).map(User::getPicture).orElse(null);
        }

        return CommentResponseDto.builder()
                .id(e.getId())
                .parentId(e.getParentId())
                .targetType(e.getTargetType())
                .targetId(e.getTargetId())
                .userId(e.getUserId())
                .username(e.getUsername())
                .userPicture(upic)
                .content(e.getContent())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .replies(replies)
                .targetTitle(targetInfo[0])
                .targetSource(targetInfo[1])
                .targetLink(targetInfo[2])
                .build();
    }

    /** 임시 상태 토큰을 검증하고, 유효하다면 탈퇴 정보를 조회 */
    @Transactional(readOnly = true)
    public WithdrawnUserDto getWithdrawnUserInfoByToken(String token) {
        if (token == null || !tokenProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
        String email = tokenProvider.getUsername(token);

        return withdrawnUserRepo.findByEmail(email)
                .map(WithdrawnUserDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "탈퇴 정보를 찾을 수 없습니다."));
    }

    /**
     * 현재 로그인된 사용자의 프로필 사진을 업로드된 파일로 변경
     * @param file 새로 업로드된 이미지 파일
     * @return 새로 업데이트된 프로필 사진 URL을 담은 DTO
     */
    @Transactional
    public ProfileImageResponseDto updateProfileImage(MultipartFile file) {
        User currentUser = currentUser();
        String oldImageUrl = currentUser.getPicture();

        // 1. 새 이미지를 'profile' 하위 폴더에 저장
        String newImageUrl = fileStorageService.storeFile(file, "profile");

        // 2. 사용자의 picture 필드를 새 이미지 URL로 업데이트하고 DB에 저장
        currentUser.setPicture(newImageUrl);
        userRepo.save(currentUser);
        log.info("사용자 ID {}의 프로필 사진이 업데이트되었습니다: {}", currentUser.getId(), newImageUrl);

        // 3. 이전 이미지가 기본 이미지가 아니었다면, 서버에서 이전 이미지 파일을 삭제
        if (oldImageUrl != null && !oldImageUrl.equals(defaultProfileImageUrl)) {
            fileStorageService.deleteFile(oldImageUrl);
        }

        return new ProfileImageResponseDto(newImageUrl);
    }

    /**
     * 현재 로그인된 사용자의 프로필 사진을 기본 이미지로 변경
     * @return 기본 프로필 사진 URL을 담은 DTO
     */
    @Transactional
    public ProfileImageResponseDto resetProfileImage() {
        User currentUser = currentUser();
        String oldImageUrl = currentUser.getPicture();

        // 1. 사용자의 picture 필드를 기본 이미지 URL로 업데이트하고 DB에 저장
        currentUser.setPicture(defaultProfileImageUrl);
        userRepo.save(currentUser);
        log.info("사용자 ID {}의 프로필 사진이 기본 이미지로 초기화되었습니다.", currentUser.getId());

        // 2. 이전 이미지가 기본 이미지가 아니었다면, 서버에서 이전 이미지 파일을 삭제
        if (oldImageUrl != null && !oldImageUrl.equals(defaultProfileImageUrl)) {
            fileStorageService.deleteFile(oldImageUrl);
        }

        return new ProfileImageResponseDto(defaultProfileImageUrl);
    }
}