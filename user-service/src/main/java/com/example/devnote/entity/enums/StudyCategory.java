package com.example.devnote.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스터디 게시판의 카테고리 Enum
 */
@Getter
@RequiredArgsConstructor
public enum StudyCategory {

    PROJECT("프로젝트"),
    CERTIFICATE("자격증"),
    ENGLISH("영어공부"),
    JOB_PREP("취업"),
    INTERVIEW("면접"),
    ETC("기타");

    private final String description;
}
