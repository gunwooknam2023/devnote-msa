package com.example.devnote.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * 탈퇴한 사용자가 재가입 유예 기간 내에 로그인을 시도했을 때 발생하는 커스텀 예외
 */
@Getter
public class UserWithdrawnException extends AuthenticationException {
    private final String email;

    public UserWithdrawnException(String email) {
        super("탈퇴한 계정입니다. 재가입 유예 기간을 확인해주세요.");
        this.email = email;
    }
}