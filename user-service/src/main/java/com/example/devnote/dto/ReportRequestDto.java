package com.example.devnote.dto;

import com.example.devnote.entity.enums.ReportReason;
import com.example.devnote.entity.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 신고 접수 API 요청을 위한 DTO
 */
@Data
public class ReportRequestDto {
    @NotNull(message = "신고 대상 타입을 지정해야 합니다.")
    private ReportTargetType targetType;

    @NotNull(message = "신고 대상 ID를 지정해야 합니다.")
    private Long targetId;

    @NotNull(message = "신고 사유를 선택해야 합니다.")
    private ReportReason reason;

    @Size(max = 1000, message = "상세 사유는 1000자 이하로 입력해주세요.")
    private String reasonDetail;
}