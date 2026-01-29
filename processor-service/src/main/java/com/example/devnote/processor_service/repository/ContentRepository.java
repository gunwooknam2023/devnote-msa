package com.example.devnote.processor_service.repository;

import com.example.devnote.processor_service.dto.CategoryCountDto;
import com.example.devnote.processor_service.entity.ContentEntity;
import com.example.devnote.processor_service.entity.ContentStatus;
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
     * 특정 카테고리에 속한 모든 콘텐츠 목록을 조회
     */
    List<ContentEntity> findByCategory(String category);

    /**
     * 모든 카테고리에 대해 콘텐츠 개수를 집계하여 반환 (TBC 포함)
     */
    @Query("SELECT new com.example.devnote.processor_service.dto.CategoryCountDto(c.category, COUNT(c.id)) " +
            "FROM ContentEntity c " +
            "WHERE c.category IS NOT NULL " + // TBC 제외 조건 삭제
            "GROUP BY c.category")
    List<CategoryCountDto> countByCategory();

    /**
     * 특정 source의 모든 카테고리에 대해 콘텐츠 개수를 집계하여 반환 (TBC 포함)
     */
    @Query("SELECT new com.example.devnote.processor_service.dto.CategoryCountDto(c.category, COUNT(c.id)) " +
            "FROM ContentEntity c " +
            "WHERE c.source = :source AND c.category IS NOT NULL " +
            "GROUP BY c.category")
    List<CategoryCountDto> countByCategoryAndSource(@Param("source") String source);

    /**
     * 특정 상태의 콘텐츠 목록 조회
     */
    List<ContentEntity> findByStatus(ContentStatus status);
}
