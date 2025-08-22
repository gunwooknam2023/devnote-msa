package com.example.devnote.service;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.ContentDto;
import com.example.devnote.dto.ReportRequestDto;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.Report;
import com.example.devnote.entity.User;
import com.example.devnote.entity.enums.ReportReason;
import com.example.devnote.entity.enums.ReportTargetType;
import com.example.devnote.repository.CommentRepository;
import com.example.devnote.repository.ReportRepository;
import com.example.devnote.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final WebClient apiGatewayClient;

    /**
     * 신고 접수
     */
    @Transactional
    public void createReport(ReportRequestDto dto, HttpServletRequest request) {
        String ipAddress = getClientIp(request);

        // 1. 24시간 내 중복 신고 방지
        checkDuplicateReport(dto.getTargetType(), dto.getTargetId(), ipAddress);

        // 2. 신고 대상 정보 스냅샷 가져오기
        String targetContentSnapshot = getTargetContentSnapshot(dto.getTargetType(), dto.getTargetId());

        // 3. 신고자 정보 확인 (회원/비회원)
        User reporter = getCurrentUser().orElse(null);

        // 4. Report 엔티티 생성
        Report report = Report.builder()
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .reason(dto.getReason())
                .targetContentSnapshot(targetContentSnapshot)
                .reporter(reporter)
                .reporterIp(ipAddress)
                .isProcessed(false)
                .build();

        //'기타' 사유에 대한 상세 내용 처리
        if (dto.getReason() == ReportReason.OTHER) {
            // 신고 사유가 '기타'인데 상세 내용이 없으면 에러 발생
            if (dto.getReasonDetail() == null || dto.getReasonDetail().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 사유가 '기타'인 경우, 상세 내용을 반드시 입력해야 합니다.");
            }
            // 상세 내용을 엔티티에 저장
            report.setReasonDetail(dto.getReasonDetail());
        }

        // 5. 최종 Report 엔티티 저장
        reportRepository.save(report);
        log.info("신규 신고가 접수되었습니다: reporterIp={}, target={}:{}", ipAddress, dto.getTargetType(), dto.getTargetId());
    }

    /**
     * 신고 처리 상태 변경(관리자용)
     */
    @Transactional
    public void processReport(Long reportId, boolean isProcessed) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."));
        report.setProcessed(isProcessed);
        reportRepository.save(report);
        log.info("신고 처리 상태 변경: reportId={}, isProcessed={}", reportId, isProcessed);
    }

    private void checkDuplicateReport(ReportTargetType targetType, Long targetId, String ipAddress) {
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        if (reportRepository.findFirstByTargetTypeAndTargetIdAndReporterIpAndCreatedAtAfterOrderByCreatedAtDesc(
                targetType, targetId, ipAddress, twentyFourHoursAgo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "24시간 내에 동일한 대상에 대해 이미 신고하셨습니다.");
        }
    }

    private String getTargetContentSnapshot(ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.CONTENT) {
            try {
                ApiResponseDto<ContentDto> response = apiGatewayClient.get()
                        .uri("/api/v1/contents/{id}", targetId)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                        .block();
                return response != null && response.getData() != null ? response.getData().getTitle() : "[삭제된 콘텐츠]";
            } catch (Exception e) {
                log.warn("신고 대상 콘텐츠 정보를 가져오는 데 실패했습니다: contentId={}", targetId);
                return "[정보 조회 실패]";
            }
        } else { // COMMENT
            return commentRepository.findById(targetId)
                    .map(CommentEntity::getContent)
                    .orElse("[삭제된 댓글]");
        }
    }

    private Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return userRepository.findByEmail(auth.getName());
        }
        return Optional.empty();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}