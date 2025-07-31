package com.example.devnote.service;

import com.example.devnote.entity.FavoriteContent;
import com.example.devnote.entity.User;
import com.example.devnote.repository.FavoriteContentRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentFavoriteService {
    private final FavoriteContentRepository favRepo;
    private final UserRepository userRepository;
    private final WebClient apiGatewayClient;

    /** 현재 요청 유저 ID (JWT subject = email) */
    private Long currentUserId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + email
                ));
        return u.getId();
    }

    /** 콘텐츠 존재 확인 */
    private void assertContentExists(Long contentId) {
        apiGatewayClient.get()
                .uri("/api/v1/contents/{id}", contentId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        resp -> Mono.error(new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Content not found: " + contentId
                        ))
                )
                .toBodilessEntity()
                .block();
    }

    /** 콘텐츠 찜 추가 */
    @Transactional
    public void add(Long contentId) {
        Long userId = currentUserId();

        assertContentExists(contentId);

        if (favRepo.findByUserIdAndContentId(userId, contentId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Content already favorited: " + contentId
            );
        }

        favRepo.save(FavoriteContent.builder()
                .userId(userId)
                .contentId(contentId)
                .build());
    }

    /** 콘텐츠 찜 삭제 */
    @Transactional
    public void remove(Long contentId) {
        Long userId = currentUserId();

        FavoriteContent fav = favRepo.findByUserIdAndContentId(userId, contentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "FavoriteContent not found: " + contentId
                ));

        favRepo.delete(fav);
    }

    /** 찜한 콘텐츠 목록 조회 */
    @Transactional(readOnly = true)
    public List<Long> list() {
        Long userId = currentUserId();
        return favRepo.findByUserId(userId)
                .stream()
                .map(FavoriteContent::getContentId)
                .toList();
    }
}