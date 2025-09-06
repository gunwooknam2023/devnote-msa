package com.example.devnote.service;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.ContentDto;
import com.example.devnote.dto.ViewHistoryRequestDto;
import com.example.devnote.entity.User;
import com.example.devnote.entity.ViewHistory;
import com.example.devnote.repository.UserRepository;
import com.example.devnote.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoryService {

    private final ViewHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final WebClient apiGatewayClient;

    /**
     * 현재 로그인된 사용자의 시청 기록을 저장하거나 업데이트
     */
    @Transactional
    public void recordViewHistory(ViewHistoryRequestDto dto) {
        User currentUser = getCurrentUser();
        Long contentId = dto.getContentId();

        // 1. 동일한 콘텐츠에 대한 시청 기록이 이미 있는지 확인
        ViewHistory history = historyRepository.findByUserAndContentId(currentUser, contentId)
                .map(existingHistory -> {
                    // 2. 기존 기록이 있다면, viewedAt 시간을 현재 시간으로 업데이트
                    existingHistory.setViewedAt(Instant.now());
                    return existingHistory;
                })
                .orElseGet(() -> ViewHistory.builder() // 3. 없다면 새로 생성
                        .user(currentUser)
                        .contentId(contentId)
                        .build());

        // 4. save를 호출하면, 새 기록은 INSERT 기존 기록은 UPDATE
        historyRepository.save(history);
    }

    /**
     * 현재 로그인된 사용자의 시청 기록 목록을 조회합니다.
     * 존재하지 않는 콘텐츠의 기록은 자동으로 삭제
     */
    @Transactional
    public Page<ContentDto> getViewHistory(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ViewHistory> historyPage = historyRepository.findByUserOrderByViewedAtDesc(currentUser, pageable);

        List<ContentDto> validContents = new ArrayList<>();
        List<ViewHistory> historiesToDelete = new ArrayList<>();

        for (ViewHistory history : historyPage.getContent()) {
            ContentDto content = fetchContentDetails(history.getContentId());

            if (content != null) {
                // 콘텐츠 정보 조회를 성공하면, 유효한 목록에 추가
                validContents.add(content);
            } else {
                // 콘텐츠 정보 조회를 실패하면(삭제된 콘텐츠), 해당 시청 기록을 삭제 대상 목록에 추가
                historiesToDelete.add(history);
            }
        }

        // 삭제 대상이 있다면 DB에서 한 번에 삭제
        if (!historiesToDelete.isEmpty()) {
            historyRepository.deleteAllInBatch(historiesToDelete);
            log.info("존재하지 않는 콘텐츠에 대한 시청 기록 {}건을 삭제했습니다.", historiesToDelete.size());
        }

        return new PageImpl<>(validContents, pageable, historyPage.getTotalElements() - historiesToDelete.size());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    /**
     * 콘텐츠 상세 정보를 조회하고, 404 Not Found 발생 시 null을 반환
     */
    private ContentDto fetchContentDetails(Long contentId) {
        try {
            ApiResponseDto<ContentDto> response = apiGatewayClient.get()
                    .uri("/api/v1/contents/{id}", contentId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                    .block();
            return (response != null) ? response.getData() : null;
        } catch (WebClientResponseException.NotFound ex) {
            log.warn("시청 기록에 포함된 콘텐츠 정보를 찾을 수 없습니다 (contentId={}).", contentId);
            return null;
        } catch (Exception e) {
            log.error("콘텐츠 정보를 가져오는 중 오류 발생 (contentId={})", contentId, e);
            return null;
        }
    }

    /**
     * 현재 로그인된 사용자의 시청 기록을 source에 따라 전체 삭제
     * @param source 삭제할 콘텐츠의 소스 ("YOUTUBE" 또는 "NEWS")
     */
    @Transactional
    public void deleteAllHistory(String source) {
        User currentUser = getCurrentUser();

        // 1. 현재 사용자의 모든 시청 기록을 DB에서 조회
        List<ViewHistory> allHistories = historyRepository.findByUser(currentUser);
        if (allHistories.isEmpty()) {
            return; // 삭제할 기록이 없으면 종료
        }

        // 2. 삭제해야 할 시청 기록만 필터링
        List<ViewHistory> historiesToDelete = allHistories.stream()
                .filter(history -> {
                    ContentDto content = fetchContentDetails(history.getContentId());
                    return content != null && source.equalsIgnoreCase(content.getSource());
                })
                .toList();

        // 3. 필터링된 기록들을 DB에서 한 번에 삭제
        if (!historiesToDelete.isEmpty()) {
            historyRepository.deleteAllInBatch(historiesToDelete);
            log.info("사용자 ID {}의 {} 시청 기록 {}건을 삭제했습니다.", currentUser.getId(), source, historiesToDelete.size());
        }
    }

    /**
     * 현재 로그인된 사용자의 특정 콘텐츠 시청 기록을 개별 삭제
     */
    @Transactional
    public void deleteHistoryByContentId(Long contentId) {
        User currentUser = getCurrentUser();
        long deletedCount = historyRepository.deleteByUserAndContentId(currentUser, contentId);

        if (deletedCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 콘텐츠에 대한 시청 기록을 찾을 수 없습니다.");
        }
        log.info("사용자 ID {}의 시청 기록이 삭제되었습니다 (contentId={})", currentUser.getId(), contentId);
    }

}