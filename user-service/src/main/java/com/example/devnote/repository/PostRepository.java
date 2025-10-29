package com.example.devnote.repository;

import com.example.devnote.entity.Post;
import com.example.devnote.entity.User;
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

    /**
     * 제목으로 검색 (전체 게시판)
     */
    Page<Post> findByTitleContaining(String keyword, Pageable pageable);

    /**
     * 제목으로 검색 (특정 게시판)
     */
    Page<Post> findByBoardTypeAndTitleContaining(BoardType boardType, String keyword, Pageable pageable);

    /**
     * 제목 또는 내용으로 검색 (전체 게시판)
     */
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Post> findByTitleOrContentContaining(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 제목 또는 내용으로 검색 (특정 게시판)
     */
    @Query("SELECT p FROM Post p WHERE p.boardType = :boardType AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> findByBoardTypeAndTitleOrContentContaining(@Param("boardType") BoardType boardType, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 게시판의 모든 게시글 조회 (정렬 적용)
     */
    Page<Post> findByBoardType(BoardType boardType, Pageable pageable);

    /**
     * 모든 게시글 조회 (정렬 적용)
     */
    Page<Post> findAll(Pageable pageable);

    /**
     * 내용으로 검색 (전체 게시판)
     */
    Page<Post> findByContentContaining(String keyword, Pageable pageable);

    /**
     * 내용으로 검색 (특정 게시판)
     */
    Page<Post> findByBoardTypeAndContentContaining(BoardType boardType, String keyword, Pageable pageable);


    /**
     * 작성자 이름으로 검색 (전체 게시판)
     */
    Page<Post> findByUser_NameContaining(String keyword, Pageable pageable);

    /**
     * 작성자 이름으로 검색 (특정 게시판)
     */
    Page<Post> findByBoardTypeAndUser_NameContaining(BoardType boardType, String keyword, Pageable pageable);

    /**
     * 특정 사용자가 작성한 게시글을 최신순으로 조회 (페이지네이션)
     */
    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 특정 사용자의 스크랩 게시글 개수
     */
    long countByUser(User user);
}
