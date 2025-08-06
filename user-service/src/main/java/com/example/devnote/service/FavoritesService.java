package com.example.devnote.service;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.ChannelSubscriptionDto;
import com.example.devnote.dto.ContentDto;
import com.example.devnote.dto.FavoritesResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritesService {
    private final ChannelFavoriteService channelFav;
    private final ContentFavoriteService contentFav;
    private final WebClient apiGatewayClient;

    /**
     * 찜한 채널 & 콘텐츠를 한 번에 조회
     *  - channelFav.list() / contentFav.list() 로 ID 리스트 가져오기
     *  - API-Gateway로 DTO(ContentDto, ChannelSubscriptionDto) 호출
     */
    public FavoritesResponseDto getAllFavorites() {
        // 1) 채널 ID → ChannelSubscriptionDto
        List<ChannelSubscriptionDto> channels = channelFav.list().stream()
                .map(chId -> apiGatewayClient.get()
                        .uri("/api/v1/channels/{id}", chId)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ChannelSubscriptionDto>>() {})
                        .block()
                        .getData()
                ).toList();

        // 2) 콘텐츠 ID → ContentDto
        List<ContentDto> contents = contentFav.list().stream()
                .map(ctId -> apiGatewayClient.get()
                        .uri("/api/v1/contents/{id}", ctId)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponseDto<ContentDto>>() {})
                        .block()
                        .getData()
                ).toList();

        // 3) DTO 조합
        return new FavoritesResponseDto(channels, contents);
    }
}