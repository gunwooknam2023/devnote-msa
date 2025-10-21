package com.example.devnote.repository;

import com.example.devnote.dto.RankedContentIdDto;
import com.example.devnote.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByContentIdAndParentIdIsNullOrderByCreatedAtAsc(Long contentId);
    List<CommentEntity> findByContentIdOrderByCreatedAtAsc(Long contentId);
    List<CommentEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);
    List<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 특정 댓글의 자식 존재 여부 */
    boolean existsByParentId(Long parentId);

    /** 특정 사용자가 작성한 총 댓글 수 (메인 댓글 + 대댓글) */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.userId = :userId")
    int countTotalCommentsByUserId(@Param("userId") Long userId);

    /** 특정 날짜에 생성된 총 댓글 수 (대댓글 포함) */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE FUNCTION('DATE', c.createdAt) = :date")
    long countByCreatedAt(@Param("date") LocalDate date);

    /**
     * 가장 많이 댓글이 달린 콘텐츠 ID와 댓글 수를 페이지네이션하여 조회
     */
    @Query("SELECT new com.example.devnote.dto.RankedContentIdDto(c.contentId, COUNT(c.id)) " +
            "FROM CommentEntity c " +
            "GROUP BY c.contentId " +
            "ORDER BY COUNT(c.id) DESC, c.contentId ASC")
    Page<RankedContentIdDto> findTopCommentedContentIds(Pageable pageable);

    /**
     * 여러 contendId에 해당하는 댓글수를 Map 형태로 한번에 조회
     */
    @Query("SELECT c.contentId as contentId, COUNT(c.id) as commentCount " +
            "FROM CommentEntity c " +
            "WHERE c.contentId IN :contentIds " +
            "GROUP BY c.contentId")
    List<Map<String, Object>> countCommentsByContentIds(@Param("contentIds") List<Long> contentIds);

    /**
     * 특정 게시글에 달린 모든 댓글들 삭제
     */
    void deleteAllByContentId(Long contentId);
}
