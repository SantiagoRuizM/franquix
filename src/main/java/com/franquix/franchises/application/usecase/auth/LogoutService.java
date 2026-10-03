package com.franquix.franchises.application.usecase.auth;

import com.franquix.franchises.domain.port.in.LogoutUseCase;
import com.franquix.franchises.domain.port.out.RefreshTokenGeneratorPort;
import com.franquix.franchises.domain.port.out.RefreshTokenRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

/** Idempotent: logging out with an unknown token, or a token of another user, silently does nothing. */
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokens;
    private final RefreshTokenGeneratorPort generator;
    private final Clock clock;

    public LogoutService(RefreshTokenRepositoryPort refreshTokens, RefreshTokenGeneratorPort generator, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.generator = generator;
        this.clock = clock;
    }

    @Override
    public Mono<Void> logout(String userId, String refreshToken) {
        return Mono.defer(() -> refreshTokens.revoke(generator.hash(refreshToken), userId, clock.instant()));
    }
}
