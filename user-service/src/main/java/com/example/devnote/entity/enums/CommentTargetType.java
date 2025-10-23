package com.example.devnote.entity.enums;

import jakarta.ws.rs.POST;

/**
 * 댓글이 달린 대상 타입 Enum
 */
public enum CommentTargetType {

    CONTENT, // 뉴스, 유튜브
    POST // 게시글 (자유게시판, Q&A, 스터디)
}
