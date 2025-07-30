package com.example.devnote.controller;

import com.example.devnote.dto.FavoriteRequestDto;
import com.example.devnote.dto.FavoriteResponseDto;
import com.example.devnote.entity.User;
import com.example.devnote.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 찜 추가 */
    @PostMapping
    public ResponseEntity<FavoriteResponseDto> addFavorite(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody FavoriteRequestDto req
    ) {
        FavoriteResponseDto dto = favoriteService.addFavorite(currentUser, req);
        return ResponseEntity.status(201).body(dto);
    }

    /** 내 찜 목록 조회 */
    @GetMapping
    public ResponseEntity<List<FavoriteResponseDto>> listFavorites(
            @AuthenticationPrincipal User currentUser
    ) {
        List<FavoriteResponseDto> list = favoriteService.listFavorites(currentUser);
        return ResponseEntity.ok(list);
    }

    /** 찜 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFavorite(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("id") Long id
    ) {
        favoriteService.deleteFavorite(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
