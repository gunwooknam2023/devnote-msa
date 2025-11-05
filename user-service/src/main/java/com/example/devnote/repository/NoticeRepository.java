package com.example.devnote.repository;

import com.example.devnote.entity.Notice;
import com.example.devnote.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 사용자가 작성한 모든 공지사항 조회 (탈퇴 처리용)
     */
    List<Notice> findByUser(User user);
}