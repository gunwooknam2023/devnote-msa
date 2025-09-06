package com.example.devnote.repository;

import com.example.devnote.entity.User;
import com.example.devnote.entity.ViewHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    /**
     * 특정 사용자와 콘텐츠 ID로 기존 시청 기록 조회 (업데이트용)
     */
    Optional<ViewHistory> findByUserAndContentId(User user, Long contentId);

    /**
     * 특정 사용자의 전체 시청 기록을 마지막 시청 시간(viewedAt)의 내림차순으로 페이지네이션하여 조회
     */
    Page<ViewHistory> findByUserOrderByViewedAtDesc(User user, Pageable pageable);

    /**
     * 특정 사용자의 모든 시청 기록을 리스트로 조회
     */
    List<ViewHistory> findByUser(User user);

    /**
     * 특정 사용자의 특정 콘텐츠에 대한 시청 기록 삭제
     */
    @Transactional
    long deleteByUserAndContentId(User user, Long contentId);
}

