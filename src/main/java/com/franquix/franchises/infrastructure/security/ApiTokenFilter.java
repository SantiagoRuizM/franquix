package com.franquix.franchises.infrastructure.security;

import com.franquix.franchises.infrastructure.entrypoint.rest.error.ProblemDetailWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Every {@code /api/**} request must send {@code Authorization: Bearer <API_TOKEN>}. Health and API
 * docs stay public. The token is compared in constant time so response timing does not leak it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiTokenFilter implements WebFilter {

    private static final String BEARER = "Bearer ";

    private final byte[] expectedToken;
    private final ProblemDetailWriter writer;

    public ApiTokenFilter(@Value("${franquix.api-token}") String apiToken, ProblemDetailWriter writer) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("franquix.api-token (API_TOKEN) must be set");
        }
        this.expectedToken = apiToken.getBytes(StandardCharsets.UTF_8);
        this.writer = writer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getPath().value().startsWith("/api/") || hasValidToken(exchange)) {
            return chain.filter(exchange);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return writer.write(exchange,
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Missing or invalid bearer token"),
                headers);
    }

    private boolean hasValidToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return false;
        }
        byte[] presented = header.substring(BEARER.length()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(presented, expectedToken);
    }
}
