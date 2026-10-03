package com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/** OAuth2-style token pair. Send {@code Authorization: Bearer <accessToken>} on API calls. */
public record TokenResponse(
        String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Access token lifetime in seconds", example = "300") long expiresIn,
        String refreshToken,
        @Schema(description = "Refresh token lifetime in seconds", example = "86400") long refreshExpiresIn) {
}
