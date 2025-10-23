package com.example.devnote.dto;

import com.example.devnote.entity.enums.CommentTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDto {

    @NotNull(message = "targetType is required")
    private CommentTargetType targetType;

    @NotNull(message = "targetId is required")
    private Long targetId;

    private Long parentId;

    @NotBlank(message = "content is required")
    @Size(max = 1000, message = "최대 1000자까지 입력 가능합니다.")
    private String content;

    /** 비회원 댓글용 */
    @Size(max = 20, message = "사용자명은 최대 20자까지 가능합니다.")
    private String username;

    @Size(max = 20, message = "비밀번호는 최대 20자까지 가능합니다.")
    private String password;
}