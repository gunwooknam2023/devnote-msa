package com.example.devnote.service;

import com.example.devnote.entity.Post;
import com.example.devnote.entity.PostLike;
import com.example.devnote.entity.User;
import com.example.devnote.entity.enums.VoteType;
import com.example.devnote.repository.PostLikeRepository;
import com.example.devnote.repository.PostRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    /**
     * 특정 게시글에 좋아요 또는 싫어요 투표를 처리
     * @param postId 대상 게시글 ID
     * @param voteType 투표 유형 (LIKE or DISLIKE)
     */
    @Transactional
    public void vote(Long postId, VoteType voteType) {
        User user = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        Optional<PostLike> existingVote = postLikeRepository.findByUserAndPost(user, post);

        // 이전에 투표한 기록이 있는지 확인
        if (existingVote.isPresent()) {
            PostLike vote = existingVote.get();
            // 이미 같은 종류로 투표했다면 (예: 좋아요 누른 글에 또 좋아요), 투표를 취소
            if (vote.getVoteType() == voteType) {
                postLikeRepository.delete(vote);
                updatePostCounts(post, voteType, -1); // 카운트 감소
            } else {
                // 다른 종류로 투표했다면 (예: 좋아요 -> 싫어요), 투표 변경
                // 기존 투표 카운트 감소
                updatePostCounts(post, vote.getVoteType(), -1);
                // 새 투표로 변경 및 카운트 증가
                vote.setVoteType(voteType);
                postLikeRepository.save(vote);
                updatePostCounts(post, voteType, 1);
            }
        } else {
            // 첫 투표일 경우, 새로 생성
            PostLike newVote = PostLike.builder()
                    .user(user)
                    .post(post)
                    .voteType(voteType)
                    .build();
            postLikeRepository.save(newVote);
            updatePostCounts(post, voteType, 1); // 카운트 증가
        }
    }

    // 게시글의 좋아요/싫어요 카운트를 업데이트하는 메서드
    private void updatePostCounts(Post post, VoteType voteType, int delta) {
        if (voteType == VoteType.LIKE) {
            post.setLikeCount(post.getLikeCount() + delta);
        } else {
            post.setDislikeCount(post.getDislikeCount() + delta);
        }
        postRepository.save(post);
    }

    // 현재 로그인한 사용자 정보를 가져오는 헬퍼 메서드
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}