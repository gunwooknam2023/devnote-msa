package com.example.devnote.repository;

import com.example.devnote.entity.Post;
import com.example.devnote.entity.PostScrap;
import com.example.devnote.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {
    
    /**
     * 특정 사용자와 게시글의 스크랩 관계 조회
     */
    Optional<PostScrap> findByUserAndPost(User user, Post post);
    
    /**
     * 특정 사용자의 스크랩한 게시글 목록 조회 (최신순)
     */
    Page<PostScrap> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * 특정 사용자가 특정 게시글을 스크랩했는지 확인
     */
    boolean existsByUserAndPost(User user, Post post);

    /**
     * 특정 사용자의 스크랩 게시글 개수
     */
    long countByUser(User user);

    /**
     * 특정 사용자의 모든 게시글 스크랩 기록 삭제
     */
    void deleteByUser(User user);
}
