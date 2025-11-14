package com.example.devnote.news_youtube_service.repository;

import com.example.devnote.news_youtube_service.entity.ChannelAddRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelAddRequestRepository extends JpaRepository<ChannelAddRequest, Long> {
    /** 생성일시 기준 내림차순 정렬 */
    Page<ChannelAddRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 소스 구분별 조회 (생성일시 기준 내림차순) */
    Page<ChannelAddRequest> findBySourceOrderByCreatedAtDesc(String source, Pageable pageable);
}
