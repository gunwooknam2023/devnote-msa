package com.example.devnote.config;

import com.example.devnote.dto.LoginResponseDto;
import com.example.devnote.entity.User;
import com.example.devnote.repository.UserRepository;
import com.example.devnote.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    /** OAuth2 로그인 성공 후 호출 */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException {
        DefaultOAuth2User oauthUser = (DefaultOAuth2User) auth.getPrincipal();
        Long userId = ((Number) oauthUser.getAttribute("id")).longValue();
        User user = userRepository.findById(userId).orElseThrow();

        String accessToken  = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getWriter(), new LoginResponseDto(accessToken, refreshToken));
    }
}
