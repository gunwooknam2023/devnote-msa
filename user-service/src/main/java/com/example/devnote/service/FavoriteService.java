package com.example.devnote.service;

import com.example.devnote.dto.FavoriteRequestDto;
import com.example.devnote.dto.FavoriteResponseDto;
import com.example.devnote.entity.Favorite;
import com.example.devnote.entity.User;
import com.example.devnote.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Transactional
    public FavoriteResponseDto addFavorite(User user, FavoriteRequestDto req) {
        Favorite fav = Favorite.builder()
                .user(user)
                .type(req.getType())
                .itemId(req.getItemId())
                .build();
        fav = favoriteRepository.save(fav);
        return new FavoriteResponseDto(fav.getId(), fav.getType(), fav.getItemId());
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponseDto> listFavorites(User user) {
        return favoriteRepository.findByUser(user).stream()
                .map(f -> new FavoriteResponseDto(f.getId(), f.getType(), f.getItemId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFavorite(User user, Long favoriteId) {
        Favorite fav = favoriteRepository.findById(favoriteId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "찜 항목을 찾을 수 없습니다: " + favoriteId));
        if (!fav.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 찜만 삭제할 수 있습니다.");
        }
        favoriteRepository.delete(fav);
    }
}