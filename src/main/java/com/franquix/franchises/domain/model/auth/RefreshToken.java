package com.franquix.franchises.domain.model.auth;

import java.time.Instant;

/**
 * Server-side record of an issued refresh token. Only the hash is stored, so a database leak does
 * not leak usable tokens.
 *
 * @param tokenHash hash of the opaque token handed to the client
 * @param userId    owner
 * @param revokedAt {@code null} while the token is usable
 */
public record RefreshToken(String tokenHash, String userId, Instant issuedAt, Instant expiresAt, Instant revokedAt) {

    public static RefreshToken issue(String tokenHash, String userId, Instant now, Instant expiresAt) {
        return new RefreshToken(tokenHash, userId, now, expiresAt, null);
    }

    public boolean revoked() {
        return revokedAt != null;
    }
}
