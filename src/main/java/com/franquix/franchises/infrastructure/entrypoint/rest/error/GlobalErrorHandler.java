package com.franquix.franchises.infrastructure.entrypoint.rest.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * Turns every error into an RFC 7807 {@code application/problem+json} response.
 *
 * <ul>
 *   <li>400 - request validation, malformed body or id, domain invariant violations</li>
 *   <li>401 - rejected credentials or refresh token (with {@code WWW-Authenticate})</li>
 *   <li>404 - unknown resource or route</li>
 *   <li>409 - duplicate name or email</li>
 *   <li>other framework statuses (405, 415, ...) are passed through</li>
 *   <li>500 - anything else, without leaking internals; the cause is logged</li>
 * </ul>
 *
 * <p>Ordered before Spring Boot's {@code DefaultErrorWebExceptionHandler} ({@code @Order(-1)}).
 */
@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private final ProblemDetailWriter writer;

    public GlobalErrorHandler(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }
        ProblemDetail problem = toProblem(error);
        log(exchange, problem, error);
        HttpHeaders headers = new HttpHeaders();
        if (error instanceof AuthenticationFailedException) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return writer.write(exchange, problem, headers);
    }

    private ProblemDetail toProblem(Throwable error) {
        return switch (error) {
            case RequestValidationException e -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
                if (!e.errors().isEmpty()) {
                    problem.setProperty("errors", e.errors());
                }
                yield problem;
            }
            case DomainValidationException e -> ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
            case AuthenticationFailedException e ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
            case NotFoundException e -> ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
            case DuplicateNameException e -> ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
            case ServerWebInputException e ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request: " + rootReason(e));
            case ResponseStatusException e -> ProblemDetail.forStatusAndDetail(
                    e.getStatusCode(), e.getReason() != null ? e.getReason() : reasonOf(e.getStatusCode()));
            default -> ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        };
    }

    private static String rootReason(ServerWebInputException error) {
        Throwable cause = error.getMostSpecificCause();
        if (cause instanceof JsonProcessingException) {
            // Jackson messages expose Java type names; keep the answer generic
            return "request body is not valid JSON or a field has the wrong type";
        }
        return error.getReason() != null ? error.getReason() : "invalid input";
    }

    private static String reasonOf(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }

    private static void log(ServerWebExchange exchange, ProblemDetail problem, Throwable error) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        if (problem.getStatus() >= 500) {
            log.error("status={} method={} path={} error={}", problem.getStatus(), method, path, error.toString(), error);
        } else {
            log.warn("status={} method={} path={} detail=\"{}\"", problem.getStatus(), method, path, problem.getDetail());
        }
    }
}
