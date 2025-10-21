package com.example.devnote.repository;

import com.example.devnote.entity.Post;
import com.example.devnote.entity.enums.BoardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 특정 게시판의 모든 게시글을 최신순으로 페이지네이션하여 조회
     */
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    /**
     * 모든 게시판의 게시글을 최신순으로 페이지네이션하여 조회
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 게시글의 조회수를 1 증가
     */
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);
}
