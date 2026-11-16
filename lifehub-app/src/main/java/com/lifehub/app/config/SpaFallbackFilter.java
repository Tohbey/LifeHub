package com.lifehub.app.config;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Serves {@code index.html} for client-side routes, so refreshing on {@code /finance} loads the PWA
 * instead of returning 404. A path is a client route when it is a GET, is not under one of the server's
 * own prefixes ({@code /api}, {@code /internal}, {@code /actuator}, the API docs and Swagger UI), and its
 * last segment has no file extension.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SpaFallbackFilter implements WebFilter {

    private static final List<String> SERVER_PREFIXES =
            List.of("/api", "/internal", "/actuator", "/openapi", "/v3/api-docs", "/swagger-ui", "/webjars");

    /** Rewrites GET requests for client-side routes to /index.html; passes everything else through unchanged. */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (exchange.getRequest().getMethod() == HttpMethod.GET && isClientRoute(path)) {
            ServerWebExchange forwarded = exchange.mutate()
                    .request(exchange.getRequest().mutate().path("/index.html").build())
                    .build();
            return chain.filter(forwarded);
        }
        return chain.filter(exchange);
    }

    /** True for paths the Angular router handles: not under a server prefix, and no file extension. */
    static boolean isClientRoute(String path) {
        for (String prefix : SERVER_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return false;
            }
        }
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !lastSegment.contains(".");
    }
}
