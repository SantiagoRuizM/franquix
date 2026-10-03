package com.franquix.franchises.infrastructure.entrypoint.rest.validation;

import java.util.Map;

/** The request is syntactically valid but its content breaks a constraint (body or path). */
public class RequestValidationException extends RuntimeException {

    private final transient Map<String, String> errors;

    public RequestValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = Map.copyOf(errors);
    }

    public static RequestValidationException invalidId(String name, String value) {
        return new RequestValidationException(
                "Invalid %s '%s'".formatted(name, value),
                Map.of(name, "must be a 24-character hexadecimal ObjectId"));
    }

    public static RequestValidationException missingBody() {
        return new RequestValidationException("Request body is required", Map.of());
    }

    /** Field name to error message; empty when the error is not tied to a field. */
    public Map<String, String> errors() {
        return errors;
    }
}
