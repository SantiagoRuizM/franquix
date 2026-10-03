package com.franquix.franchises.domain.port.out;

import com.franquix.franchises.domain.model.auth.RefreshToken;
import java.time.Instant;
import reactor.core.publisher.Mono;

/** Persistence port for refresh tokens, identified by the hash of the opaque token. */
public interface RefreshTokenRepositoryPort {

    Mono<RefreshToken> create(RefreshToken token);

    /**
     * Atomically marks a usable token as revoked, so it can be exchanged exactly once even under
     * concurrent requests.
     *
     * @return the token as it was before revocation, or empty when it is unknown, expired or already revoked
     */
    Mono<RefreshToken> consume(String tokenHash, Instant now);

    /** @return the token whatever its state, or empty when unknown */
    Mono<RefreshToken> findByHash(String tokenHash);

    /** Revokes the token only if it belongs to the user. */
    Mono<Void> revoke(String tokenHash, String userId, Instant now);

    /** Revokes every token of the user (password change, token reuse). */
    Mono<Void> revokeAllForUser(String userId, Instant now);
}
