package com.example.devnote.service;

import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.CommentLike;
import com.example.devnote.entity.User;
import com.example.devnote.entity.enums.VoteType;
import com.example.devnote.repository.CommentLikeRepository;
import com.example.devnote.repository.CommentRepository;
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
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    /**
     * 특정 댓글에 좋아요 또는 싫어요 투표를 처리
     * @param commentId 대상 댓글 ID
     * @param voteType 투표 유형 (LIKE or DISLIKE)
     */
    @Transactional
    public void vote(Long commentId, VoteType voteType) {
        User user = getCurrentUser();
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        Optional<CommentLike> existingVote = commentLikeRepository.findByUserAndComment(user, comment);

        // 이전에 투표한 기록이 있는지 확인
        if (existingVote.isPresent()) {
            CommentLike vote = existingVote.get();
            // 이미 같은 종류로 투표했다면 (예: 좋아요 누른 글에 또 좋아요), 투표를 취소
            if (vote.getVoteType() == voteType) {
                commentLikeRepository.delete(vote);
                updateCommentCounts(comment, voteType, -1); // 카운트 감소
            } else {
                // 다른 종류로 투표했다면 (예: 좋아요 -> 싫어요), 투표 변경
                // 기존 투표 카운트 감소
                updateCommentCounts(comment, vote.getVoteType(), -1);
                // 새 투표로 변경 및 카운트 증가
                vote.setVoteType(voteType);
                commentLikeRepository.save(vote);
                updateCommentCounts(comment, voteType, 1);
            }
        } else {
            // 첫 투표일 경우, 새로 생성
            CommentLike newVote = CommentLike.builder()
                    .user(user)
                    .comment(comment)
                    .voteType(voteType)
                    .build();
            commentLikeRepository.save(newVote);
            updateCommentCounts(comment, voteType, 1); // 카운트 증가
        }
    }

    // 댓글의 좋아요/싫어요 카운트를 업데이트하는 메서드
    private void updateCommentCounts(CommentEntity comment, VoteType voteType, int delta) {
        if (voteType == VoteType.LIKE) {
            comment.setLikeCount(comment.getLikeCount() + delta);
        } else {
            comment.setDislikeCount(comment.getDislikeCount() + delta);
        }
        commentRepository.save(comment);
    }

    // 현재 로그인한 사용자 정보를 가져오는 헬퍼 메서드
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}