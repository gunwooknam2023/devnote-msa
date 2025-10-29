package com.example.devnote.service;

import com.example.devnote.dto.PostDetailResponseDto;
import com.example.devnote.dto.PostListResponseDto;
import com.example.devnote.dto.PostRequestDto;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.Post;
import com.example.devnote.entity.User;
import com.example.devnote.entity.enums.BoardType;
import com.example.devnote.entity.enums.CommentTargetType;
import com.example.devnote.entity.enums.PostSortType;
import com.example.devnote.entity.enums.SearchType;
import com.example.devnote.repository.CommentRepository;
import com.example.devnote.repository.PostLikeRepository;
import com.example.devnote.repository.PostRepository;
import com.example.devnote.repository.PostScrapRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final FileStorageService fileStorageService;

    /**
     * 현재 인증된 사용자 정보 추출 메서드
     */
    private User getCurrentUser(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    /**
     * 게시글 생성
     */
    @Transactional
    public PostDetailResponseDto createPost(PostRequestDto dto){
        User user = getCurrentUser();

        // 스터디 게시글일 경우 카테고리 유효성 검사
        if (dto.getBoardType() == BoardType.STUDY){
            if (dto.getStudyCategory() == null || dto.getStudyMethod() == null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "스터디 게시글은 스터디 종류와 진행 방식을 모두 선택해야 합니다.");
            }
        }

        Post post = Post.builder()
                .user(user)
                .boardType(dto.getBoardType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .studyCategory(dto.getStudyCategory())
                .studyMethod(dto.getStudyMethod())
                .build();

        Post savedPost = postRepository.save(post);
        return toDetailDto(savedPost);
    }

    /**
     * 게시글 목록 조회
     */
    /**
     * 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getPosts(BoardType boardType, PostSortType sortType, SearchType searchType, String keyword, Pageable pageable){
        // 1. 검색 여부 확인
        boolean isSearching = keyword != null && !keyword.trim().isEmpty();

        // 2. MOST_COMMENTED 정렬인지 확인
        boolean isCommentSorted = sortType == PostSortType.MOST_COMMENTED;

        // 3. MOST_COMMENTED가 아닌 경우: 일반 정렬
        Page<Post> postPage;
        if (!isCommentSorted) {
            // 정렬 기준 설정 및 PageRequest 생성
            if (sortType == null) {
                sortType = PostSortType.LATEST;
            }

            Sort.Direction direction = sortType.getSortDirection().equals("ASC")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            Sort sort = Sort.by(direction, sortType.getSortField());

            Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    sort
            );

            // 게시글 조회 (검색 타입 및 검색 여부에 따라 분기)
            if (isSearching && searchType != null) {
                // 검색 모드
                switch (searchType) {
                    case TITLE:
                        // 제목으로 검색
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndTitleContaining(boardType, keyword, sortedPageable);
                        } else {
                            postPage = postRepository.findByTitleContaining(keyword, sortedPageable);
                        }
                        break;
                    case CONTENT:
                        // 내용으로 검색
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndContentContaining(boardType, keyword, sortedPageable);
                        } else {
                            postPage = postRepository.findByContentContaining(keyword, sortedPageable);
                        }
                        break;
                    case TITLE_CONTENT:
                        // 제목+내용으로 검색
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndTitleOrContentContaining(boardType, keyword, sortedPageable);
                        } else {
                            postPage = postRepository.findByTitleOrContentContaining(keyword, sortedPageable);
                        }
                        break;
                    case AUTHOR:
                        // 작성자로 검색
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndUser_NameContaining(boardType, keyword, sortedPageable);
                        } else {
                            postPage = postRepository.findByUser_NameContaining(keyword, sortedPageable);
                        }
                        break;
                    default:
                        // 기본값: 제목+내용으로 검색
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndTitleOrContentContaining(boardType, keyword, sortedPageable);
                        } else {
                            postPage = postRepository.findByTitleOrContentContaining(keyword, sortedPageable);
                        }
                        break;
                }
            } else {
                // 일반 조회 모드
                if (boardType != null) {
                    postPage = postRepository.findByBoardType(boardType, sortedPageable);
                } else {
                    postPage = postRepository.findAll(sortedPageable);
                }
            }
        } else {
            // MOST_COMMENTED 정렬: 먼저 정렬 없이 모든 게시글 조회
            Pageable unsortedPageable = PageRequest.of(0, Integer.MAX_VALUE);

            // 검색 타입에 따라 조회
            if (isSearching && searchType != null) {
                // 검색 모드 (정렬 없음)
                switch (searchType) {
                    case TITLE:
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndTitleContaining(boardType, keyword, unsortedPageable);
                        } else {
                            postPage = postRepository.findByTitleContaining(keyword, unsortedPageable);
                        }
                        break;
                    case CONTENT:
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndContentContaining(boardType, keyword, unsortedPageable);
                        } else {
                            postPage = postRepository.findByContentContaining(keyword, unsortedPageable);
                        }
                        break;
                    case TITLE_CONTENT:
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndTitleOrContentContaining(boardType, keyword, unsortedPageable);
                        } else {
                            postPage = postRepository.findByTitleOrContentContaining(keyword, unsortedPageable);
                        }
                        break;
                    case AUTHOR:
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndUser_NameContaining(boardType, keyword, unsortedPageable);
                        } else {
                            postPage = postRepository.findByUser_NameContaining(keyword, unsortedPageable);
                        }
                        break;
                    default:
                        if (boardType != null) {
                            postPage = postRepository.findByBoardTypeAndTitleOrContentContaining(boardType, keyword, unsortedPageable);
                        } else {
                            postPage = postRepository.findByTitleOrContentContaining(keyword, unsortedPageable);
                        }
                        break;
                }
            } else {
                // 일반 조회 모드 (정렬 없음)
                if (boardType != null) {
                    postPage = postRepository.findByBoardType(boardType, unsortedPageable);
                } else {
                    postPage = postRepository.findAll(unsortedPageable);
                }
            }
        }

        // 4. 게시글 댓글수 가져오기
        List<Post> allPosts = new ArrayList<>(postPage.getContent());
        Map<Long, Long> commentCountMap = commentRepository.countCommentsByPostIds(
                        allPosts.stream().map(Post::getId).collect(Collectors.toList())
                ).stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("postId")).longValue(),
                        map -> ((Number) map.get("commentCount")).longValue()
                ));

        // 5. 정렬방식이 MOST_COMMENTED 일때 댓글 수 기준으로 정렬
        if (isCommentSorted) {
            allPosts.sort((p1, p2) -> {
                long count1 = commentCountMap.getOrDefault(p1.getId(), 0L);
                long count2 = commentCountMap.getOrDefault(p2.getId(), 0L);
                return Long.compare(count2, count1);
            });
        }

        // 6. 페이지네이션 적용
        Page<Post> finalPage;
        if (isCommentSorted) {
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allPosts.size());
            List<Post> pagedPosts = allPosts.subList(start, end);
            finalPage = new PageImpl<>(pagedPosts, pageable, allPosts.size());
        } else {
            finalPage = postPage;
        }

        return finalPage.map(post -> toListDto(post, commentCountMap.getOrDefault(post.getId(), 0L)));
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostDetailResponseDto getPostById(Long postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        // 조회수 증가
        postRepository.incrementViewCount(postId);
        post.setViewCount(post.getViewCount() + 1);

        return toDetailDto(post);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostDetailResponseDto updatePost(Long postId, PostRequestDto dto){
        User user = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!Objects.equals(post.getUser().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글을 수정할 권한이 없습니다.");
        }

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());

        // 게시판 타입은 수정 X
        if (post.getBoardType() == BoardType.STUDY) {
            if (dto.getStudyCategory() == null || dto.getStudyMethod() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "스터디 게시글은 스터디 종류와 진행 방식을 모두 선택해야 합니다.");
            }
            post.setStudyCategory(dto.getStudyCategory());
            post.setStudyMethod(dto.getStudyMethod());
        }

        Post updatedPost = postRepository.save(post);
        return toDetailDto(updatedPost);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long postId){
        User user = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!Objects.equals(post.getUser().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글을 삭제할 권한이 없습니다.");
        }

        // 1. 본문 내용에서 이미지 URL 파싱 후 실제 파일 삭제
        deleteImagesFromContent(post.getContent());

        // 2. 게시글에 달린 댓글 삭제 (POST 타입으로 지정)
        commentRepository.deleteAllByTargetTypeAndTargetId(CommentTargetType.POST, postId);

        // 3. 게시글 삭제
        postRepository.delete(post);
    }

    /**
     * Q&A 답변 채택
     */
    @Transactional
    public PostDetailResponseDto adoptAnswer(Long postId, Long commentId){
        User user = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "질문 게시글을 찾을 수 없습니다."));

        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "답변 댓글을 찾을 수 없습니다."));

        // 1. Q&A 게시글인지 확인
        if (post.getBoardType() != BoardType.QNA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Q&A 게시글만 답변을 채택할 수 있습니다.");
        }
        // 2. 게시글 작성자 본인인지 확인
        if (!Objects.equals(post.getUser().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "질문 작성자만 답변을 채택할 수 있습니다.");
        }
        // 3. 채택하려는 댓글이 해당 게시글의 댓글인지 확인
        if(comment.getTargetType() != CommentTargetType.POST
                || !Objects.equals(comment.getTargetId(), postId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 게시글에 존재하지 않는 댓글입니다.");
        }
        // 4. 자기 자신의 댓글은 채택 불가
        if (Objects.equals(comment.getUserId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신의 댓글은 채택할 수 없습니다.");
        }

        post.setAdopted(true);
        post.setAdoptedCommentId(commentId);

        return toDetailDto(postRepository.save(post));
    }

    /**
     * 이미지 업로드
     */
    public String uploadImage(MultipartFile imageFile){
        return fileStorageService.storeFile(imageFile, "posts");
    }

    /**
     * 본문에서 이미지 URL 파싱 후 파일 삭제
     */
    private void deleteImagesFromContent(String content) {
        if (content == null || content.isBlank()) return;
        Pattern pattern = Pattern.compile("!\\[[^\\]]*\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            fileStorageService.deleteFile(matcher.group(1));
        }
    }

    /**
     * 사용자의 게시글 투표 상태 조회
     */
    public String getUserVoteType(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Post not found: " + postId));

        return postLikeRepository.findByUserAndPost(user, post)
                .map(like -> like.getVoteType().name())
                .orElse("NONE");
    }

    /**
     * 현재 사용자의 게시글 투표 상태 조회 (인증된 사용자용)
     */
    public String getCurrentUserVoteType(Long postId) {
        try {
            User user = getCurrentUser();
            return getUserVoteType(postId, user);
        } catch (Exception e) {
            return "NONE";  // 인증되지 않은 사용자는 NONE
        }
    }

    /**
     * 현재 사용자의 게시글 스크랩 상태 조회 (인증된 사용자용)
     */
    public boolean getCurrentUserScrapStatus(Long postId) {
        try {
            User user = getCurrentUser();
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
            return postScrapRepository.existsByUserAndPost(user, post);
        } catch (Exception e) {
            return false; // 인증되지 않은 사용자는 false
        }
    }


    /**
     * Post 엔티티를 PostDetailResponseDto로 변환
     */
    private PostDetailResponseDto toDetailDto(Post post) {
        return PostDetailResponseDto.builder()
                .id(post.getId())
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .scrapCount(post.getScrapCount())
                .userVoteType(getCurrentUserVoteType(post.getId()))
                .isScraped(getCurrentUserScrapStatus(post.getId()))
                .authorId(post.getUser().getId())
                .authorName(post.getUser().getName())
                .authorPicture(post.getUser().getPicture())
                .viewCount(post.getViewCount())
                .isAdopted(post.isAdopted())
                .adoptedCommentId(post.getAdoptedCommentId())
                .studyCategory(post.getStudyCategory())
                .studyMethod(post.getStudyMethod())
                .isRecruiting(post.isRecruiting())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * Post 엔티티를 PostListResponseDto로 변환
     */
    private PostListResponseDto toListDto(Post post, long commentCount) {
        return PostListResponseDto.builder()
                .id(post.getId())
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .authorId(post.getUser().getId())
                .authorName(post.getUser().getName())
                .authorPicture(post.getUser().getPicture())
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .scrapCount(post.getScrapCount())
                .isAdopted(post.isAdopted())
                .studyCategory(post.getStudyCategory())
                .studyMethod(post.getStudyMethod())
                .isRecruiting(post.isRecruiting())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
