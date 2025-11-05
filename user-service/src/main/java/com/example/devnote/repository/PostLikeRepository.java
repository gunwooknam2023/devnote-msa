package com.example.devnote.repository;

import com.example.devnote.entity.Post;
import com.example.devnote.entity.PostLike;
import com.example.devnote.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    /**
     * 특정 사용자가 특정 게시글에 투표한 내역을 찾기
     */
    Optional<PostLike> findByUserAndPost(User user, Post post);

    /**
     * 특정 사용자의 모든 게시글 좋아요/싫어요 기록 삭제
     */
    void deleteByUser(User user);
}