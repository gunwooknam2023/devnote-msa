package com.example.devnote.processor_service.service;

import com.example.devnote.processor_service.dto.CommentRequestDto;
import com.example.devnote.processor_service.dto.CommentResponseDto;
import com.example.devnote.processor_service.dto.CommentUpdateDto;
import com.example.devnote.processor_service.entity.CommentEntity;
import com.example.devnote.processor_service.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 댓글 생성, 조회, 수정, 삭제 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto req) {
        // 비밀번호 BCrypt 해시
        String hash = passwordEncoder.encode(req.getPassword());

        // 엔티티 생성, 저장
        CommentEntity comment = CommentEntity.builder()
                .contentId(req.getContentId())
                .parentId(req.getParentId())
                .username(req.getUsername())
                .passwordHash(hash)
                .content(req.getContent())
                .build();

        comment = commentRepository.save(comment);
        log.info("Created comment id={}", comment.getId());

        // DTO 반환
        return toDto(comment, Collections.emptyList());
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponseDto updateComment(Long id, CommentUpdateDto req) {
        CommentEntity comment = commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("댓글이 존재하지 않습니다: " + id));

        // 비밀번호 확인
        if (!passwordEncoder.matches(req.getPassword(), comment.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 내용 변경
        comment.setContent(req.getContent());
        comment = commentRepository.save(comment);
        log.info("Updated comment id={}", comment.getId());

        // DTO 반환
        List<CommentResponseDto> replies = listReplies(comment.getId());
        return toDto(comment, replies);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long id, String password) {
        CommentEntity comment = commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("댓글이 존재하지 않습니다: " + id));

        // 비밀번호 확인
        if(!passwordEncoder.matches(password, comment.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 댓글 삭제
        commentRepository.delete(comment);
        log.info("Deleted comment id={}", id);
    }

    /**
     * 해당 콘텐츠에 달린 댓글 전체 조회 (댓글, 대댓글)
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDto> listCommentsByContent(Long contentId) {
        // 댓글 조회
        List<CommentEntity> roots = commentRepository
                .findByContentIdAndParentIdIsNullOrderByCreatedAtAsc(contentId);

        // 댓글에 대한 대댓글 조회
        return roots.stream()
                .map(root -> toDto(root, listReplies(root.getId())))
                .collect(Collectors.toList());
    }

    /** 특정 댓글의 대댓글 리스트 DTO */
    private List<CommentResponseDto> listReplies(Long parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId)
                .stream()
                .map(ent -> toDto(ent, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    /** Entity → DTO 변환 */
    private CommentResponseDto toDto(CommentEntity ent, List<CommentResponseDto> replies) {
        return CommentResponseDto.builder()
                .id(ent.getId())
                .parentId(ent.getParentId())
                .contentId(ent.getContentId())
                .username(ent.getUsername())
                .content(ent.getContent())
                .createdAt(ent.getCreatedAt())
                .updatedAt(ent.getUpdatedAt())
                .replies(replies)
                .build();
    }
}
