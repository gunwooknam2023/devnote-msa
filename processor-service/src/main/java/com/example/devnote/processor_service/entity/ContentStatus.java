package com.example.devnote.processor_service.entity;

/**
 * 콘텐츠 상태
 * - ACTIVE: 정상 노출
 * - HIDDEN: 삭제/비공개 감지되어 숨김 처리됨
 */
public enum ContentStatus {
    ACTIVE,
    HIDDEN
}
