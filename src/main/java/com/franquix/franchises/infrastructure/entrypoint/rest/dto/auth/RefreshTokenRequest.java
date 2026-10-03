package com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Body of refresh and logout. */
public record RefreshTokenRequest(
        @Schema(description = "Opaque refresh token returned by login or refresh",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        String refreshToken) {
}
