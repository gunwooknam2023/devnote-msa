package com.example.devnote.news_youtube_service.service;

import com.example.devnote.news_youtube_service.dto.ChannelAddRequestDto;
import com.example.devnote.news_youtube_service.dto.ChannelAddRequestResponseDto;
import com.example.devnote.news_youtube_service.entity.ChannelAddRequest;
import com.example.devnote.news_youtube_service.repository.ChannelAddRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChannelAddRequestService {

    private final ChannelAddRequestRepository channelAddRequestRepository;

    /**
     * 채널 추가 요청 생성 (인증 불필요)
     */
    @Transactional
    public ChannelAddRequestResponseDto createRequest(ChannelAddRequestDto dto) {
        // 1. 소스 구분 검증 (YOUTUBE 또는 NEWS만 허용)
        String source = dto.getSource().toUpperCase();
        if (!"YOUTUBE".equals(source) && !"NEWS".equals(source)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "소스 구분은 'YOUTUBE' 또는 'NEWS'만 가능합니다."
            );
        }

        // 2. ChannelAddRequest 엔티티 생성
        ChannelAddRequest request = ChannelAddRequest.builder()
                .channelName(dto.getChannelName())
                .link(dto.getLink())
                .source(source)
                .requestReason(dto.getRequestReason())
                .build();

        // 3. DB에 저장
        ChannelAddRequest savedRequest = channelAddRequestRepository.save(request);

        // 4. 응답 DTO로 변환하여 반환
        return toResponseDto(savedRequest);
    }

    /**
     * ChannelAddRequest 엔티티를 ChannelAddRequestResponseDto로 변환
     */
    private ChannelAddRequestResponseDto toResponseDto(ChannelAddRequest request) {
        return ChannelAddRequestResponseDto.builder()
                .id(request.getId())
                .channelName(request.getChannelName())
                .link(request.getLink())
                .source(request.getSource())
                .requestReason(request.getRequestReason())
                .createdAt(request.getCreatedAt())
                .build();
    }
}