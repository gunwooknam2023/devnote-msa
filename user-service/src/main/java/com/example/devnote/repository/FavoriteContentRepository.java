package com.example.devnote.repository;

import com.example.devnote.entity.FavoriteContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteContentRepository extends JpaRepository<FavoriteContent, Long> {
    Optional<FavoriteContent> findByUserIdAndContentId(Long userId, Long contentId);
    List<FavoriteContent> findByUserId(Long userId);
    void deleteByContentId(Long contentId);
}
