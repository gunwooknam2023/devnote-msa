package com.example.devnote.processor_service.repository;

import com.example.devnote.processor_service.dto.CategoryCountDto;
import com.example.devnote.processor_service.entity.ContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<ContentEntity, Long>, JpaSpecificationExecutor<ContentEntity> {
    Optional<ContentEntity> findBySourceAndLink(String source, String link);
    List<ContentEntity> findBySourceAndCategory(String source, String category);
    long countBySource(String source);

    @Query("SELECT COUNT(c) FROM ContentEntity c WHERE FUNCTION('DATE', c.createdAt) = :date")
    long countByCreatedAt(@Param("date") LocalDate date);

    /**
     * 모든 카테고리에 대해 콘텐츠 개수를 집계하여 반환
     */
    @Query("SELECT new com.example.devnote.processor_service.dto.CategoryCountDto(c.category, COUNT(c.id)) " +
            "FROM ContentEntity c " +
            "WHERE c.category IS NOT NULL AND c.category != 'TBC' " +
            "GROUP BY c.category")
    List<CategoryCountDto> countByCategory();
}
