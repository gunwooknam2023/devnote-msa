package com.example.devnote.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostSortType {
    LATEST("최신순", "createdAt", "DESC"),
    OLDEST("오래된순", "createdAt", "ASC"),
    MOST_LIKED("좋아요 높은순", "likeCount", "DESC"),
    MOST_SCRAPPED("스크랩 많은순", "scrapCount", "DESC"),
    MOST_VIEWED("조회수 높은순", "viewCount", "DESC"),
    MOST_COMMENTED("댓글 많은순", "commentCount", "DESC");

    private final String description;
    private final String sortField;
    private final String sortDirection;
}