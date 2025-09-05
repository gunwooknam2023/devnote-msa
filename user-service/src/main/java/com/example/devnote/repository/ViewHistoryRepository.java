package com.example.devnote.repository;

import com.example.devnote.entity.User;
import com.example.devnote.entity.ViewHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    /**
     * 사용자와 콘텐츠 ID로 기존 시청 기록 조회 (업데이트용)
     */
    Optional<ViewHistory> findByUserAndContentId(User user, Long contentId);

    /**
     * 사용자의 전체 시청 기록을 마지막 시청 시간(viewedAt)의 내림차순으로 페이지네이션하여 조회
     */
    Page<ViewHistory> findByUserOrderByViewedAtDesc(User user, Pageable pageable);
}
