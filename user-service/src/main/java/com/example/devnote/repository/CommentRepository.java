package com.example.devnote.repository;

import com.example.devnote.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByContentIdAndParentIdIsNullOrderByCreatedAtAsc(Long contentId);
    List<CommentEntity> findByContentIdOrderByCreatedAtAsc(Long contentId);
    List<CommentEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);
    List<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 특정 댓글의 자식 존재 여부 */
    boolean existsByParentId(Long parentId);

    /** 특정 사용자가 작성한 총 댓글 수 (메인 댓글 + 대댓글) */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.userId = :userId")
    int countTotalCommentsByUserId(@Param("userId") Long userId);

    /** 특정 날짜에 생성된 총 댓글 수 (대댓글 포함) */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE FUNCTION('DATE', c.createdAt) = :date")
    long countByCreatedAt(@Param("date") LocalDate date);
}
