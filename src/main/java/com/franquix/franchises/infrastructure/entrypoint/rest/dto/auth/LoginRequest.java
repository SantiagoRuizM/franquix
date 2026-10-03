package com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin@franquix.local", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String email,

        @Schema(example = "change-me-local-only", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String password) {
}
