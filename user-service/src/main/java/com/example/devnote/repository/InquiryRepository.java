package com.example.devnote.repository;

import com.example.devnote.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 사용자가 작성한 모든 문의사항 조회 (탈퇴 처리용)
     */
    List<Inquiry> findByUserId(Long userId);
}