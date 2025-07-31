package com.example.devnote.service;

import com.example.devnote.entity.FavoriteChannel;
import com.example.devnote.entity.User;
import com.example.devnote.repository.FavoriteChannelRepository;
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
public class ChannelFavoriteService {
    private final FavoriteChannelRepository favRepo;
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

    /** 채널 존재 확인 */
    private void assertChannelExists(Long chSubId) {
        apiGatewayClient.get()
                .uri("/api/v1/channels/{id}", chSubId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        resp -> Mono.error(new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found: " + chSubId
                        ))
                )
                .toBodilessEntity()
                .block();
    }

    /** 채널 찜 추가 */
    @Transactional
    public void add(Long chSubId) {
        Long userId = currentUserId();

        assertChannelExists(chSubId);

        if (favRepo.findByUserIdAndChannelSubscriptionId(userId, chSubId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Channel already favorited: " + chSubId
            );
        }

        favRepo.save(FavoriteChannel.builder()
                .userId(userId)
                .channelSubscriptionId(chSubId)
                .build());
    }

    /** 채널 찜 삭제 */
    @Transactional
    public void remove(Long chSubId) {
        Long userId = currentUserId();

        FavoriteChannel fav = favRepo.findByUserIdAndChannelSubscriptionId(userId, chSubId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "FavoriteChannel not found: " + chSubId
                ));

        favRepo.delete(fav);
    }

    /** 찜한 채널 목록 조회 */
    @Transactional(readOnly = true)
    public List<Long> list() {
        Long userId = currentUserId();
        return favRepo.findByUserId(userId)
                .stream()
                .map(FavoriteChannel::getChannelSubscriptionId)
                .toList();
    }
}
