package com.franquix.franchises.application.usecase.auth;

import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.model.auth.AuthTokens;
import com.franquix.franchises.domain.model.auth.RefreshToken;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.in.RefreshSessionUseCase;
import com.franquix.franchises.domain.port.out.RefreshTokenGeneratorPort;
import com.franquix.franchises.domain.port.out.RefreshTokenRepositoryPort;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import reactor.core.publisher.Mono;

public class RefreshSessionService implements RefreshSessionUseCase {

    private final RefreshTokenRepositoryPort refreshTokens;
    private final RefreshTokenGeneratorPort generator;
    private final UserRepositoryPort users;
    private final SessionIssuer sessions;
    private final Clock clock;

    public RefreshSessionService(
            RefreshTokenRepositoryPort refreshTokens,
            RefreshTokenGeneratorPort generator,
            UserRepositoryPort users,
            SessionIssuer sessions,
            Clock clock) {
        this.refreshTokens = refreshTokens;
        this.generator = generator;
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public Mono<AuthTokens> refresh(String refreshToken) {
        return Mono.defer(() -> {
            String tokenHash = generator.hash(refreshToken);
            Instant now = clock.instant();
            return refreshTokens.consume(tokenHash, now)
                    .flatMap(consumed -> users.findById(consumed.userId()))
                    .filter(User::active)
                    .flatMap(sessions::open)
                    .switchIfEmpty(Mono.defer(() -> revokeAllOnReuse(tokenHash, now))
                            .then(Mono.error(AuthenticationFailedException::invalidRefreshToken)));
        });
    }

    /** A rotated token presented again means it leaked: end every session of its owner. */
    private Mono<Void> revokeAllOnReuse(String tokenHash, Instant now) {
        return refreshTokens.findByHash(tokenHash)
                .filter(RefreshToken::revoked)
                .flatMap(reused -> refreshTokens.revokeAllForUser(reused.userId(), now));
    }
}
