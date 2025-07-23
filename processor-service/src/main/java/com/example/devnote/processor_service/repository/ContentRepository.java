package com.example.devnote.processor_service.repository;

import com.example.devnote.processor_service.entity.ContentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<ContentEntity, Long>, JpaSpecificationExecutor<ContentEntity> {
    Optional<ContentEntity> findBySourceAndLink(String source, String link);
}
