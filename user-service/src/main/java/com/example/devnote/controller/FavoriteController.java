package com.example.devnote.controller;

import com.example.devnote.dto.ApiResponseDto;
import com.example.devnote.dto.FavoritesResponseDto;
import com.example.devnote.service.ChannelFavoriteService;
import com.example.devnote.service.ContentFavoriteService;
import com.example.devnote.service.FavoritesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final ChannelFavoriteService channelFav;
    private final ContentFavoriteService contentFav;
    private final FavoritesService favoritesService;

    /**
     * 찜한 채널 & 콘텐츠를 한 번에 조회
     * - 로그인된 유저ID로 즐겨찾기 테이블에서 ID 리스트 조회
     * - API-Gateway 통해 DTO(ContentDto, ChannelSubscriptionDto) 가져와 조합
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponseDto<FavoritesResponseDto>> getAllFavorites() {
        FavoritesResponseDto dto = favoritesService.getAllFavorites();
        return ResponseEntity.ok(
                ApiResponseDto.<FavoritesResponseDto>builder()
                        .message("Fetched all favorites")
                        .statusCode(200)
                        .data(dto)
                        .build()
        );
    }

    // 채널 찜 추가
    @PostMapping("/channels/{id}")
    public ResponseEntity<ApiResponseDto<Void>> addChannel(@PathVariable Long id) {
        channelFav.add(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.<Void>builder()
                        .message("Channel favorited")
                        .statusCode(HttpStatus.CREATED.value())
                        .data(null)
                        .build());
    }

    // 채널 찜 삭제
    @DeleteMapping("/channels/{id}")
    public ResponseEntity<ApiResponseDto<Void>> removeChannel(@PathVariable Long id) {
        channelFav.remove(id);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("Channel unfavorited")
                        .statusCode(HttpStatus.OK.value())
                        .data(null)
                        .build());
    }

    // 채널 찜 목록
    @GetMapping("/channels")
    public ResponseEntity<ApiResponseDto<List<Long>>> listChannels() {
        List<Long> list = channelFav.list();
        return ResponseEntity.ok(
                ApiResponseDto.<List<Long>>builder()
                        .message("Fetched favorited channels")
                        .statusCode(HttpStatus.OK.value())
                        .data(list)
                        .build());
    }

    // 콘텐츠 찜 추가
    @PostMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> addContent(@PathVariable Long id) {
        contentFav.add(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.<Void>builder()
                        .message("Content favorited")
                        .statusCode(HttpStatus.CREATED.value())
                        .data(null)
                        .build());
    }

    // 콘텐츠 찜 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> removeContent(@PathVariable Long id) {
        contentFav.remove(id);
        return ResponseEntity.ok(
                ApiResponseDto.<Void>builder()
                        .message("Content unfavorited")
                        .statusCode(HttpStatus.OK.value())
                        .data(null)
                        .build());
    }

    // 콘텐츠 찜 목록
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<Long>>> listContents() {
        List<Long> list = contentFav.list();
        return ResponseEntity.ok(
                ApiResponseDto.<List<Long>>builder()
                        .message("Fetched favorited contents")
                        .statusCode(HttpStatus.OK.value())
                        .data(list)
                        .build());
    }
}
