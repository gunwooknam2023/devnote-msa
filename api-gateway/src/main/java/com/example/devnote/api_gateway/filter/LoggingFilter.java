//package com.example.devnote.api_gateway.filter;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.cloud.gateway.route.Route;
//import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
///**
// * 모든 요청/응답에 대해 로그를 남기고,
// * 각 요청을 stats-service로 비동기 전송해 트래픽 전송
// */
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class LoggingFilter implements GlobalFilter, Ordered {
//    private final WebClient.Builder lb;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getURI().getPath();
//
//        HttpMethod methodObj = request.getMethod();
//        String method = (methodObj != null) ? methodObj.name() : "-";
//
//        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
//        String routeId = (route != null) ? route.getId() : "-";
//
//        log.info("[Gateway][{}] Incoming  → {} {}", routeId, method, request.getURI());
//
//        return chain.filter(exchange)
//                // 성공/에러/취소 모두 한 번 호출
//                .doFinally(signalType -> {
//                    HttpStatusCode status = exchange.getResponse().getStatusCode();
//                    String statusText = (status != null) ? status.toString() : "-";
//                    log.info("[Gateway][{}] Outgoing ← {} {}", routeId, statusText, request.getURI());
//
//                    // 루프 방지: 트래픽 트랙 엔드포인트는 제외
//                    if (path.startsWith("/api/v1/traffic/track")) {
//                        return;
//                    }
//
//                    // 비동기 fire-and-forget: 메서드/경로 전달
//                    lb.build()
//                            .post()
//                            .uri(uriBuilder -> uriBuilder
//                                    .scheme("http")
//                                    .host("stats-service")
//                                    .path("/api/v1/traffic/track")
//                                    .queryParam("m", method)
//                                    .queryParam("p", path)
//                                    .build())
//                            .retrieve()
//                            .toBodilessEntity()
//                            .subscribe();
//                });
//    }
//
//    /**
//     * 필터 실행 순서를 정의
//     */
//    @Override
//    public int getOrder() {
//        return -1;
//    }
//}
