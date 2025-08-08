package com.example.devnote.config;

import com.example.devnote.security.CustomOAuth2UserService;
import com.example.devnote.security.OAuth2AuthenticationFailureHandler;
import com.example.devnote.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 (OAuth2 + JWT)
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final JwtRefreshFilter refreshFilter;
    private final CustomOAuth2UserService oauth2UserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final OAuth2AuthenticationFailureHandler failureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI().startsWith("/api/")
                        )
                )
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**", "/api/v1/auth/**", "/api/v1/comments/**")
                        .permitAll()
                        .requestMatchers("/api/v1/users/*/profile").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(u -> u.userService(oauth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(refreshFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}