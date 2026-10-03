package com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String currentPassword,

        @Schema(description = "At least 12 characters, at most 72 bytes, not only digits, not common, "
                + "not similar to the email", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String newPassword) {
}
