package com.franquix.franchises.domain.model.auth;

import java.time.Instant;

/** What a successful login or refresh hands to the client. */
public record AuthTokens(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {
}
