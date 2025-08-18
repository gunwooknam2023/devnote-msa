package com.example.devnote.service;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.ChannelSubscriptionDto;
import com.example.devnote.dto.ContentDto;
import com.example.devnote.dto.FavoritesResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoritesService {
    private final ChannelFavoriteService channelFav;
    private final ContentFavoriteService contentFav;
    private final WebClient apiGatewayClient;

    /**
     * 찜한 채널 & 콘텐츠를 한 번에 조회
     * - 404 Not Found 오류 발생 시 해당 항목은 제외하고 계속 진행하도록 수정
     */
    public FavoritesResponseDto getAllFavorites() {
        // 1) 찜한 채널 ID -> ChannelSubscriptionDto
        List<ChannelSubscriptionDto> channels = channelFav.list().stream()
                .map(chId -> {
                    try {
                        return apiGatewayClient.get()
                                .uri("/api/v1/channels/{id}", chId)
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ChannelSubscriptionDto>>() {})
                                .block()
                                .getData();
                    } catch (WebClientResponseException.NotFound ex) {
                        // 404 오류가 발생하면, 로그를 남기고 null을 반환하여 계속 진행
                        log.warn("Favorited channel not found (id={}), skipping.", chId);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2) 찜한 콘텐츠 ID -> ContentDto
        List<ContentDto> contents = contentFav.list().stream()
                .map(ctId -> {
                    try {
                        return apiGatewayClient.get()
                                .uri("/api/v1/contents/{id}", ctId)
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                                .block()
                                .getData();
                    } catch (WebClientResponseException.NotFound ex) {
                        log.warn("Favorited content not found (id={}), skipping.", ctId);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3) DTO 조합
        return new FavoritesResponseDto(channels, contents);
    }
}