package com.example.devnote.repository;

import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.CommentLike;
import com.example.devnote.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    /**
     * 특정 사용자가 특정 댓글에 투표한 내역을 찾기
     */
    Optional<CommentLike> findByUserAndComment(User user, CommentEntity comment);
}
