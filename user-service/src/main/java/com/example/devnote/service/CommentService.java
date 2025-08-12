package com.example.devnote.service;

import com.example.devnote.dto.CommentRequestDto;
import com.example.devnote.dto.CommentResponseDto;
import com.example.devnote.dto.CommentUpdateDto;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.User;
import com.example.devnote.repository.CommentRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebClient apiGatewayClient;

    /** 댓글 생성 (회원/비회원 공용) */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto req) {
        assertContentExists(req.getContentId());

        // 대댓글 작성시 댓글 id 검증
        if (req.getParentId() != null) {
            CommentEntity parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Parent comment not found: " + req.getParentId()));
            if (!parent.getContentId().equals(req.getContentId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Parent comment does not belong to contentId=" + req.getContentId());
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username;
        String passwordHash = null;

        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            String email = auth.getName();
            User u = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "User not found: " + email));
            userId = u.getId();
            username = u.getName();
        } else {
            if (req.getUsername() == null || req.getPassword() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Anonymous comments require username & password");
            }
            username = req.getUsername();
            passwordHash = passwordEncoder.encode(req.getPassword());
        }

        CommentEntity ent = CommentEntity.builder()
                .contentId(req.getContentId())
                .parentId(req.getParentId())
                .userId(userId)
                .username(username)
                .passwordHash(passwordHash)
                .content(req.getContent())
                .build();

        ent = commentRepository.save(ent);

        // 회원 댓글인 경우 활동점수 증가
        if (userId != null) {
            userRepository.incrementActivityScore(userId);
        }

        List<CommentResponseDto> replies = listReplies(ent.getId());
        CommentResponseDto dto = toDto(ent, replies);
        return dto;
    }

    /** 댓글 수정 */
    @Transactional
    public CommentResponseDto updateComment(Long id, CommentUpdateDto req) {
        CommentEntity ent = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Comment not found: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ent.getUserId() != null) {
            // 회원 댓글
            if (auth == null
                    || !auth.isAuthenticated()
                    || auth instanceof AnonymousAuthenticationToken) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Login required");
            }
            String email = auth.getName();
            User u = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "User not found: " + email));
            if (!Objects.equals(u.getId(), ent.getUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not your comment");
            }
        } else {
            // 비회원 댓글
            if (req.getPassword() == null
                    || !passwordEncoder.matches(req.getPassword(), ent.getPasswordHash())) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid password");
            }
        }

        ent.setContent(req.getContent());
        ent = commentRepository.save(ent);

        List<CommentResponseDto> replies = listReplies(ent.getId());
        CommentResponseDto dto = toDto(ent, replies);
        return dto;
    }

    /** 댓글 삭제 */
    @Transactional
    public void deleteComment(Long id, String password) {
        CommentEntity ent = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Comment not found: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ent.getUserId() != null) {
            // 회원 댓글
            if (auth == null
                    || !auth.isAuthenticated()
                    || auth instanceof AnonymousAuthenticationToken) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Login required");
            }
            String email = auth.getName();
            User u = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "User not found: " + email));
            if (!Objects.equals(u.getId(), ent.getUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not your comment");
            }
        } else {
            // 비회원 댓글
            if (password == null
                    || !passwordEncoder.matches(password, ent.getPasswordHash())) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid password");
            }
        }

        // 회원 댓글인 경우 활동점수 감소
        if (ent.getUserId() != null) {
            userRepository.decrementActivityScore(ent.getUserId());
        }

        commentRepository.delete(ent);
    }

    /** 콘텐츠별 댓글 전체 조회 (루트 댓글) */
    @Transactional(readOnly = true)
    public List<CommentResponseDto> listCommentsByContent(Long contentId) {
        // 1) 모든 댓글 한 번에 가져오기 (루트+대댓글 전체)
        List<CommentEntity> all = commentRepository.findByContentIdOrderByCreatedAtAsc(contentId);

        // 2) userId 수집 → 최신 사용자 정보 일괄 조회
        Set<Long> userIds = all.stream()
                .map(CommentEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId, u -> u));

        // 3) id → DTO 초기화
        Map<Long, CommentResponseDto> dtoMap = new LinkedHashMap<>();
        for (CommentEntity e : all) {
            String displayName = e.getUserId() != null
                    ? Optional.ofNullable(userMap.get(e.getUserId()))
                    .map(User::getName).orElse(e.getUsername())
                    : e.getUsername();
            String displayPicture = e.getUserId() != null
                    ? Optional.ofNullable(userMap.get(e.getUserId()))
                    .map(User::getPicture).orElse(null)
                    : null;

            dtoMap.put(e.getId(), CommentResponseDto.builder()
                    .id(e.getId())
                    .parentId(e.getParentId())
                    .contentId(e.getContentId())
                    .userId(e.getUserId())
                    .username(displayName)
                    .userPicture(displayPicture)
                    .content(e.getContent())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .replies(new ArrayList<>())
                    .build());
        }

        // 4) 부모-자식 연결
        List<CommentResponseDto> roots = new ArrayList<>();
        for (CommentEntity e : all) {
            CommentResponseDto dto = dtoMap.get(e.getId());
            if (e.getParentId() == null) {
                roots.add(dto);
            } else {
                CommentResponseDto parent = dtoMap.get(e.getParentId());
                if (parent != null) parent.getReplies().add(dto);
            }
        }

        return roots;
    }

    /** 대댓글 조회 */
    private List<CommentResponseDto> listReplies(Long parentId) {
        List<CommentEntity> children = commentRepository
                .findByParentIdOrderByCreatedAtAsc(parentId);
        return children.stream()
                .map(ent -> toDto(ent, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    /** Entity → DTO */
    private CommentResponseDto toDto(
            CommentEntity e,
            List<CommentResponseDto> replies
    ) {
        CommentResponseDto dto = CommentResponseDto.builder()
                .id(e.getId())
                .parentId(e.getParentId())
                .contentId(e.getContentId())
                .userId(e.getUserId())
                .username(e.getUsername())
                .content(e.getContent())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .replies(replies)
                .build();
        return dto;
    }

    /** 여러 콘텐츠의 댓글 수를 일괄 조회 (대댓글 포함) */
    @Transactional(readOnly = true)
    public Map<Long, Integer> getCommentCounts(List<Long> contentIds) {
        Map<Long, Integer> result = new HashMap<>();

        for (Long contentId : contentIds) {
            List<CommentEntity> comments = commentRepository.findByContentIdAndParentIdIsNullOrderByCreatedAtAsc(contentId);
            int totalCount = 0;

            for (CommentEntity comment : comments) {
                totalCount++;
                totalCount += countReplies(comment.getId());
            }

            result.put(contentId, totalCount);
        }

        return result;
    }

    /** 특정 댓글의 대댓글 수를 재귀적으로 계산 */
    private int countReplies(Long parentId) {
        List<CommentEntity> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
        int count = replies.size();

        for (CommentEntity reply : replies) {
            count += countReplies(reply.getId());
        }

        return count;
    }

    /** 콘텐츠 존재 검증 */
    private void assertContentExists(Long contentId) {
        apiGatewayClient.get()
                .uri("/api/v1/contents/{id}", contentId)
                .retrieve()
                .onStatus(s -> s.value()==404,
                        resp -> Mono.error(new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Content not found: " + contentId)))
                .toBodilessEntity()
                .block();
    }
}
