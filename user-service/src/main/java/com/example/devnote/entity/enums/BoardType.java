package com.example.devnote.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시판 종류 정의 Enum
 * (자유게시판, Q&A, 스터디)
 */
@Getter
@RequiredArgsConstructor
public enum BoardType {

    FREE_BOARD("자유게시판"),
    QNA("Q&A"),
    STUDY("스터디");

    private final String description;
}
