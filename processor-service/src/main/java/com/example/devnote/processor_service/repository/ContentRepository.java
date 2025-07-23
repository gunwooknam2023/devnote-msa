package com.example.devnote.processor_service.repository;

import com.example.devnote.processor_service.entity.ContentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<ContentEntity, Long> {
    Optional<ContentEntity> findBySourceAndLink(String source, String link);
    List<ContentEntity> findByCategoryOrderByIdDesc(String category, Pageable pageable);
    List<ContentEntity>
    findByCategoryAndIdLessThanOrderByIdDesc(String category, Long id, Pageable pageable);

    List<ContentEntity> findBySourceOrderByIdDesc(String source, Pageable page);
    List<ContentEntity> findBySourceAndIdLessThanOrderByIdDesc(String source, Long cursor, Pageable page);
}
