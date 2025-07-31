package com.example.devnote.service;

import com.example.devnote.dto.CommentRequestDto;
import com.example.devnote.dto.CommentResponseDto;
import com.example.devnote.dto.CommentUpdateDto;
import com.example.devnote.entity.CommentEntity;
import com.example.devnote.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepo;
    private final PasswordEncoder passwordEncoder;

    /** 댓글 생성 */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto req) {
        // 비밀번호 해싱
        String hash = passwordEncoder.encode(req.getPassword());

        CommentEntity ent = CommentEntity.builder()
                .contentId(req.getContentId())
                .parentId(req.getParentId())
                .username(req.getUsername())
                .passwordHash(hash)
                .content(req.getContent())
                .build();

        ent = commentRepo.save(ent);
        return toDto(ent, Collections.emptyList());
    }

    /** 댓글 수정 */
    @Transactional
    public CommentResponseDto updateComment(Long id, CommentUpdateDto req) {
        CommentEntity ent = commentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Comment not found: " + id));

        if (!passwordEncoder.matches(req.getPassword(), ent.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        ent.setContent(req.getContent());
        ent = commentRepo.save(ent);

        List<CommentResponseDto> replies = listReplies(ent.getId());
        return toDto(ent, replies);
    }

    /** 댓글 삭제 */
    @Transactional
    public void deleteComment(Long id, String password) {
        CommentEntity ent = commentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Comment not found: " + id));

        if (!passwordEncoder.matches(password, ent.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        commentRepo.delete(ent);
    }

    /** 특정 콘텐츠 댓글 전체 조회 */
    @Transactional(readOnly = true)
    public List<CommentResponseDto> listCommentsByContent(Long contentId) {
        List<CommentEntity> roots = commentRepo
                .findByContentIdAndParentIdIsNullOrderByCreatedAtAsc(contentId);

        return roots.stream()
                .map(root -> toDto(root, listReplies(root.getId())))
                .collect(Collectors.toList());
    }

    private List<CommentResponseDto> listReplies(Long parentId) {
        return commentRepo.findByParentIdOrderByCreatedAtAsc(parentId)
                .stream()
                .map(ent -> toDto(ent, Collections.emptyList()))
                .collect(Collectors.toList());
    }

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
