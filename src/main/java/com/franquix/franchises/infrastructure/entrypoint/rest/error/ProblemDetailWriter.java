package com.franquix.franchises.infrastructure.entrypoint.rest.error;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Writes RFC 7807 responses. Shared by the global error handler, the security entry points and the
 * rate limiter, so every error, including those raised before routing, has the same shape.
 */
@Component
public class ProblemDetailWriter {

    private final List<HttpMessageWriter<?>> messageWriters;
    private final Clock clock;

    public ProblemDetailWriter(ServerCodecConfigurer codecs, Clock clock) {
        this.messageWriters = codecs.getWriters();
        this.clock = clock;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String detail) {
        return write(exchange, ProblemDetail.forStatusAndDetail(status, detail), new HttpHeaders());
    }

    public Mono<Void> write(ServerWebExchange exchange, ProblemDetail problem, HttpHeaders headers) {
        problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        problem.setProperty("timestamp", Instant.now(clock));
        return ServerResponse.status(problem.getStatus())
                .headers(responseHeaders -> responseHeaders.addAll(headers))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem)
                .flatMap(response -> response.writeTo(exchange, new ResponseContext()));
    }

    private final class ResponseContext implements ServerResponse.Context {

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return messageWriters;
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return List.of();
        }
    }
}
