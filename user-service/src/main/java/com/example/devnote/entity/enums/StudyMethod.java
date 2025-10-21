package com.example.devnote.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스터디 진행 방식 Enum
 * (온라인, 오프라인, 혼합)
 */
@Getter
@RequiredArgsConstructor
public enum StudyMethod {

    ONLINE("온라인"),
    OFFLINE("오프라인"),
    HYBRID("혼합");

    private final String description;
}
