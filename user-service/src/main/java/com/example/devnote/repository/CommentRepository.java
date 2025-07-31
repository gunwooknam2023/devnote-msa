package com.example.devnote.repository;

import com.example.devnote.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByContentIdAndParentIdIsNullOrderByCreatedAtAsc(Long contentId);
    List<CommentEntity> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
