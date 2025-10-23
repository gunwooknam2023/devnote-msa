package com.example.devnote.service;

import com.example.devnote.dto.PostDetailResponseDto;
import com.example.devnote.dto.PostListResponseDto;
import com.example.devnote.dto.PostRequestDto;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.Post;
import com.example.devnote.entity.User;
import com.example.devnote.entity.enums.BoardType;
import com.example.devnote.entity.enums.CommentTargetType;
import com.example.devnote.repository.CommentRepository;
import com.example.devnote.repository.PostRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getPosts(BoardType boardType, Pageable pageable){
        Page<Post> postPage = (boardType != null)
                ? postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, pageable)
                : postRepository.findAllByOrderByCreatedAtDesc(pageable);

        // 게시글 댓글수 가져오기 (POST 타입으로 조회)
        List<Long> postIds = postPage.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Long> commentCountMap = commentRepository.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("postId")).longValue(),
                        map -> ((Number) map.get("commentCount")).longValue()
                ));

        return postPage.map(post -> toListDto(post, commentCountMap.getOrDefault(post.getId(), 0L)));
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
     * Post 엔티티를 PostDetailResponseDto로 변환
     */
    private PostDetailResponseDto toDetailDto(Post post) {
        return PostDetailResponseDto.builder()
                .id(post.getId())
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .content(post.getContent())
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
                .authorName(post.getUser().getName())
                .authorPicture(post.getUser().getPicture())
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .isAdopted(post.isAdopted())
                .studyCategory(post.getStudyCategory())
                .isRecruiting(post.isRecruiting())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
