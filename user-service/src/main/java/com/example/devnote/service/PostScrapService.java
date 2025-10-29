package com.example.devnote.service;

import com.example.devnote.dto.PostListResponseDto;
import com.example.devnote.entity.Post;
import com.example.devnote.entity.PostScrap;
import com.example.devnote.entity.User;
import com.example.devnote.repository.PostRepository;
import com.example.devnote.repository.PostScrapRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostScrapService {

    private final PostRepository postRepository;
    private final PostScrapRepository postScrapRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 스크랩/스크랩 취소 처리
     */
    @Transactional
    public void toggleScrap(Long postId) {
        User user = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        Optional<PostScrap> existingScrap = postScrapRepository.findByUserAndPost(user, post);

        if (existingScrap.isPresent()) {
            // 이미 스크랩한 경우, 스크랩 취소
            postScrapRepository.delete(existingScrap.get());
            updatePostScrapCount(post, -1); // 카운트 감소
        } else {
            // 스크랩하지 않은 경우, 스크랩 추가
            PostScrap newScrap = PostScrap.builder()
                    .user(user)
                    .post(post)
                    .build();
            postScrapRepository.save(newScrap);
            updatePostScrapCount(post, 1); // 카운트 증가
        }
    }

    /**
     * 사용자의 스크랩한 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getScrapedPosts(Pageable pageable) {
        User user = getCurrentUser();
        Page<PostScrap> scrapPage = postScrapRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        return scrapPage.map(scrap -> {
            Post post = scrap.getPost();
            return PostListResponseDto.builder()
                    .id(post.getId())
                    .boardType(post.getBoardType())
                    .title(post.getTitle())
                    .authorId(post.getUser().getId())
                    .authorName(post.getUser().getName())
                    .authorPicture(post.getUser().getPicture())
                    .viewCount(post.getViewCount())
                    .commentCount(0L) // TODO: 실제 댓글 수 조회 필요시 추가
                    .likeCount(post.getLikeCount())
                    .dislikeCount(post.getDislikeCount())
                    .scrapCount(post.getScrapCount())
                    .isAdopted(post.isAdopted())
                    .studyCategory(post.getStudyCategory())
                    .studyMethod(post.getStudyMethod())
                    .isRecruiting(post.isRecruiting())
                    .createdAt(post.getCreatedAt())
                    .build();
        });
    }

    /**
     * 특정 게시글의 스크랩 수 업데이트
     */
    private void updatePostScrapCount(Post post, int delta) {
        post.setScrapCount(post.getScrapCount() + delta);
        postRepository.save(post);
    }

    /**
     * 현재 로그인한 사용자 정보를 가져오는 헬퍼 메서드
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
