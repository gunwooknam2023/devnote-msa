package com.example.devnote.processor_service.repository;

import com.example.devnote.processor_service.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    /** 해당 콘텐츠에 달린 모든 댓글 조회 */
    List<CommentEntity> findByContentIdAndParentIdIsNullOrderByCreatedAtAsc(Long contentId);

    /** 특정 댓글의 모든 대댓글 조회 */
    List<CommentEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
