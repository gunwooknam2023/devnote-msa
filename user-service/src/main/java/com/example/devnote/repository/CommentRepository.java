package com.example.devnote.repository;

import com.example.devnote.dto.RankedContentIdDto;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.enums.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByTargetTypeAndTargetIdAndParentIdIsNullOrderByCreatedAtAsc(
            CommentTargetType targetType, Long targetId);

    List<CommentEntity> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
            CommentTargetType targetType, Long targetId);

    List<CommentEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);
    List<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByParentId(Long parentId);

    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.userId = :userId")
    int countTotalCommentsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE FUNCTION('DATE', c.createdAt) = :date")
    long countByCreatedAt(@Param("date") LocalDate date);

    // CONTENT 타입만 랭킹 집계
    @Query("SELECT new com.example.devnote.dto.RankedContentIdDto(c.targetId, COUNT(c.id)) " +
            "FROM CommentEntity c " +
            "WHERE c.targetType = 'CONTENT' " +
            "GROUP BY c.targetId " +
            "ORDER BY COUNT(c.id) DESC, c.targetId ASC")
    Page<RankedContentIdDto> findTopCommentedContentIds(Pageable pageable);

    // CONTENT 타입의 댓글 수만 집계
    @Query("SELECT c.targetId as contentId, COUNT(c.id) as commentCount " +
            "FROM CommentEntity c " +
            "WHERE c.targetType = 'CONTENT' AND c.targetId IN :contentIds " +
            "GROUP BY c.targetId")
    List<Map<String, Object>> countCommentsByContentIds(@Param("contentIds") List<Long> contentIds);

    // POST 타입의 댓글 수 집계
    @Query("SELECT c.targetId as postId, COUNT(c.id) as commentCount " +
            "FROM CommentEntity c " +
            "WHERE c.targetType = 'POST' AND c.targetId IN :postIds " +
            "GROUP BY c.targetId")
    List<Map<String, Object>> countCommentsByPostIds(@Param("postIds") List<Long> postIds);

    // targetType과 targetId로 삭제
    void deleteAllByTargetTypeAndTargetId(CommentTargetType targetType, Long targetId);
}