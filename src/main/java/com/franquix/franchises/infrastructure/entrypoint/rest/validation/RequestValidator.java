package com.franquix.franchises.infrastructure.entrypoint.rest.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

/**
 * Bean Validation for functional endpoints. {@code @Valid} only works with annotated controllers,
 * so handlers call this component explicitly.
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    /** Reads, requires and validates the request body. */
    public <T> Mono<T> body(ServerRequest request, Class<T> type) {
        return request.bodyToMono(type)
                .switchIfEmpty(Mono.error(RequestValidationException::missingBody))
                .flatMap(this::validate);
    }

    /**
     * @return the path variable when it is a valid MongoDB ObjectId
     * @throws RequestValidationException otherwise
     */
    public String objectId(ServerRequest request, String name) {
        String value = request.pathVariable(name);
        if (!ObjectId.isValid(value)) {
            throw RequestValidationException.invalidId(name, value);
        }
        return value;
    }

    private <T> Mono<T> validate(T body) {
        Set<ConstraintViolation<T>> violations = validator.validate(body);
        if (violations.isEmpty()) {
            return Mono.just(body);
        }
        Map<String, String> errors = new TreeMap<>();
        violations.forEach(violation ->
                errors.merge(violation.getPropertyPath().toString(), violation.getMessage(), (a, b) -> a + "; " + b));
        return Mono.error(new RequestValidationException("Request validation failed", errors));
    }
}
