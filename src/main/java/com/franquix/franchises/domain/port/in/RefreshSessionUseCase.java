package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.auth.AuthTokens;
import reactor.core.publisher.Mono;

/**
 * Exchanges a refresh token for a new token pair. The presented token is rotated (single use); if a
 * token that was already rotated is presented again, every session of its user is revoked.
 */
@FunctionalInterface
public interface RefreshSessionUseCase {

    Mono<AuthTokens> refresh(String refreshToken);
}
