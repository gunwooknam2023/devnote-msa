package com.example.devnote.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 요청 헤더의 Authorization 토큰 검증
 */
@Component
@Slf4j
public class AuthFilter implements GatewayFilter, Ordered {
    /**
     * 요청 처리 필터 메서드
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !validateToken(token)) {
            log.warn("[Gateway] Unauthorized request to {}", exchange.getRequest().getURI());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    /**
     * 토큰 검증 메서드
     */
    private boolean validateToken(String token) {
        log.debug("[Gateway] Validating token: {}", token);
        // TODO: 실제 토큰 검증 로직 (추후 작성)
        return true;
    }

    /**
     * 필터 실행 순서
     */
    @Override
    public int getOrder() {
        return 0;  // 로깅 필터 다음으로 실행
    }
}
