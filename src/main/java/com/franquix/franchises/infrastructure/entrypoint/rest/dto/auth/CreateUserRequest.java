package com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth;

import com.franquix.franchises.domain.model.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @Schema(example = "viewer@franquix.local", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String email,

        @Schema(example = "Store Viewer", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String name,

        @Schema(description = "At least 12 characters, at most 72 bytes, not only digits, not common, "
                + "not similar to the email", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String password,

        @Schema(example = "VIEWER", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "is required")
        Role role) {
}
