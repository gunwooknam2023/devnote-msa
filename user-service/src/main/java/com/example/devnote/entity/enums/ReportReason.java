package com.example.devnote.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    SPAM("스팸 또는 광고"),
    INAPPROPRIATE_CONTENT("선정적이거나 부적절한 내용"),
    HATE_SPEECH("욕설 또는 혐오 발언"),
    COPYRIGHT_INFRINGEMENT("저작권 침해"),
    OTHER("기타");

    private final String description;
}
