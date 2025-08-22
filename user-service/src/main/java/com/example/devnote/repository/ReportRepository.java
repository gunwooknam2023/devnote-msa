package com.example.devnote.repository;

import com.example.devnote.entity.Report;
import com.example.devnote.entity.enums.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 특정 대상에 대해 특정 IP가 특정 시간 이후에 신고한 내역이 있는지 확인
     * (중복 신고 방지용 쿼리)
     */
    Optional<Report> findFirstByTargetTypeAndTargetIdAndReporterIpAndCreatedAtAfterOrderByCreatedAtDesc(
            ReportTargetType targetType, Long targetId, String reporterIp, Instant after);
}