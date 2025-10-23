package com.example.devnote.service;

import com.example.devnote.dto.CommentRequestDto;
import com.example.devnote.dto.CommentResponseDto;
import com.example.devnote.dto.CommentUpdateDto;
import com.example.devnote.dto.ContentStatsUpdateDto;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.entity.User;
import com.example.devnote.entity.enums.CommentTargetType;
import com.example.devnote.repository.CommentRepository;
import com.example.devnote.repository.PostRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebClient apiGatewayClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 댓글 생성 (회원/비회원 공용) */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto req) {
        // targetType에 따라 다른 검증
        if (req.getTargetType() == CommentTargetType.CONTENT) {
            assertContentExists(req.getTargetId());
        } else if (req.getTargetType() == CommentTargetType.POST) {
            assertPostExists(req.getTargetId());
        }

        // 대댓글 작성시 댓글 id 검증
        if (req.getParentId() != null) {
            CommentEntity parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Parent comment not found: " + req.getParentId()));
            if (!parent.getTargetType().equals(req.getTargetType())
                    || !parent.getTargetId().equals(req.getTargetId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Parent comment does not belong to the same target");
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
                .targetType(req.getTargetType())
                .targetId(req.getTargetId())
                .parentId(req.getParentId())
                .userId(userId)
                .username(username)
                .passwordHash(passwordHash)
                .content(req.getContent())
                .build();

        ent = commentRepository.save(ent);

        // CONTENT 타입일 때만 Kafka 이벤트 발행
        if (req.getTargetType() == CommentTargetType.CONTENT) {
            ContentStatsUpdateDto message = ContentStatsUpdateDto.builder()
                    .contentId(req.getTargetId())
                    .commentDelta(1)
                    .build();
            kafkaTemplate.send("content-stats-update", message);

            kafkaTemplate.send("comment.created", String.valueOf(ent.getId()));
        }

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

        // CONTENT 타입일 때만 Kafka 이벤트 발행
        if (ent.getTargetType() == CommentTargetType.CONTENT) {
            ContentStatsUpdateDto message = ContentStatsUpdateDto.builder()
                    .contentId(ent.getTargetId())
                    .commentDelta(-1)
                    .build();
            kafkaTemplate.send("content-stats-update", message);
        }

        // 회원 댓글인 경우 활동점수 감소
        if (ent.getUserId() != null) {
            userRepository.decrementActivityScore(ent.getUserId());
        }

        // 자식 댓글 존재 여부
        boolean hasChildren = commentRepository.existsByParentId(ent.getId());

        if(hasChildren) {
            // 소프트 삭제
            ent.setDeleted(true);
            ent.setContent(null);

            commentRepository.save(ent);
        } else {
            // 하드 삭제
            Long pareneId = ent.getParentId();
            commentRepository.delete(ent);

            // 부모 자동 정리
            cleanupSoftDeletedAncestor(pareneId);
        }
    }

    /**
     * 부모 자동 정리
     * - isDeleted 가 true 이고, 더이상 자식이 없으면 해당 부모 하드 삭제
     */
    private void cleanupSoftDeletedAncestor(Long maybeParentId) {
        if (maybeParentId == null) return;

        Optional<CommentEntity> opt = commentRepository.findById(maybeParentId);
        if (opt.isEmpty()) return;

        CommentEntity parent = opt.get();

        if (parent.isDeleted() && !commentRepository.existsByParentId(parent.getId())) {
            Long next = parent.getParentId();
            commentRepository.delete(parent);

            // 연쇄 정리
            cleanupSoftDeletedAncestor(next);
        }
    }

    /** 콘텐츠별 댓글 전체 조회 (루트 댓글) */
    @Transactional(readOnly = true)
    public List<CommentResponseDto> listCommentsByTarget(CommentTargetType targetType, Long targetId) {
        // 1) 모든 댓글 한 번에 가져오기 (루트+대댓글 전체)
        List<CommentEntity> all = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId);

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
        // DTO 변환 시 targetType에 따라 다른 정보 조회
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

            // 내용이 소프트 삭제된 경우 null
            String displayContent = e.isDeleted() ? null : e.getContent();

            dtoMap.put(e.getId(), CommentResponseDto.builder()
                    .id(e.getId())
                    .parentId(e.getParentId())
                    .targetType(e.getTargetType())
                    .targetId(e.getTargetId())
                    .userId(e.getUserId())
                    .username(displayName)
                    .userPicture(displayPicture)
                    .content(displayContent)
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .replies(new ArrayList<>())
                    .likeCount(e.getLikeCount())
                    .dislikeCount(e.getDislikeCount())
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

    /** Post 존재 검증 */
    private void assertPostExists(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Post not found: " + postId));
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
    private CommentResponseDto toDto(CommentEntity e, List<CommentResponseDto> replies) {
        CommentResponseDto dto = CommentResponseDto.builder()
                .id(e.getId())
                .parentId(e.getParentId())
                .targetType(e.getTargetType())
                .targetId(e.getTargetId())
                .userId(e.getUserId())
                .username(e.getUsername())
                .content(e.getContent())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .replies(replies)
                .likeCount(e.getLikeCount())
                .dislikeCount(e.getDislikeCount())
                .build();
        return dto;
    }

    /** 여러 콘텐츠의 댓글 수를 일괄 조회 (대댓글 포함) */
    @Transactional(readOnly = true)
    public Map<Long, Integer> getCommentCounts(List<Long> contentIds) {
        Map<Long, Integer> result = new HashMap<>();

        for (Long contentId : contentIds) {
            List<CommentEntity> comments = commentRepository
                    .findByTargetTypeAndTargetIdAndParentIdIsNullOrderByCreatedAtAsc(
                            CommentTargetType.CONTENT, contentId);
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

    /**
     * 특정 날짜에 생성된 댓글 수를 반환합니다 (내부 통계용).
     */
    public long countByDay(LocalDate date) {
        return commentRepository.countByCreatedAt(date);
    }
}
