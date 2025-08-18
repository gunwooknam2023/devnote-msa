package com.example.devnote.repository;

import com.example.devnote.dto.RankedContentIdDto;
import com.example.devnote.entity.FavoriteContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteContentRepository extends JpaRepository<FavoriteContent, Long> {
    Optional<FavoriteContent> findByUserIdAndContentId(Long userId, Long contentId);
    List<FavoriteContent> findByUserId(Long userId);
    void deleteByContentId(Long contentId);
    /**
     * 가장 많이 찜한 콘텐츠 ID와 찜 수를 페이지네이션하여 조회
     */
    @Query("SELECT new com.example.devnote.dto.RankedContentIdDto(fc.contentId, COUNT(fc.id)) " +
            "FROM FavoriteContent fc " +
            "GROUP BY fc.contentId " +
            "ORDER BY COUNT(fc.id) DESC, fc.contentId ASC")
    Page<RankedContentIdDto> findTopFavoritedContentIds(Pageable pageable);
}
