package com.example.devnote.repository;

import com.example.devnote.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}