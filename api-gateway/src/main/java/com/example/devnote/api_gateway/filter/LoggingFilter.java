package com.example.devnote.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 모든 요청/응답에 대해 로그를 남기는 글로벌 필터
 */
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {
    /**
     * 요청/응답 로깅 필터 메서드
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        log.info("[Gateway] Incoming  → {} {}", req.getMethod(), req.getURI());

        return chain.filter(exchange)
                .doOnSuccess(done ->
                        log.info("[Gateway] Outgoing ← {} {}",
                                exchange.getResponse().getStatusCode(), req.getURI())
                );
    }

    /**
     * 필터 실행 순서를 정의
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
