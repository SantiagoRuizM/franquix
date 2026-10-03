package com.franquix.franchises.application.usecase.auth;

import com.franquix.franchises.domain.model.auth.AuthTokens;
import com.franquix.franchises.domain.model.auth.RefreshToken;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.out.AccessTokenIssuerPort;
import com.franquix.franchises.domain.port.out.RefreshTokenGeneratorPort;
import com.franquix.franchises.domain.port.out.RefreshTokenRepositoryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import reactor.core.publisher.Mono;

/** Opens a session: one access token plus one stored, single-use refresh token. */
public class SessionIssuer {

    private final AccessTokenIssuerPort accessTokens;
    private final RefreshTokenGeneratorPort generator;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public SessionIssuer(
            AccessTokenIssuerPort accessTokens,
            RefreshTokenGeneratorPort generator,
            RefreshTokenRepositoryPort refreshTokens,
            Duration refreshTokenTtl,
            Clock clock) {
        this.accessTokens = accessTokens;
        this.generator = generator;
        this.refreshTokens = refreshTokens;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    public Mono<AuthTokens> open(User user) {
        return Mono.defer(() -> {
            Instant now = clock.instant();
            Instant refreshExpiresAt = now.plus(refreshTokenTtl);
            return Mono.zip(accessTokens.issue(user), generator.generate())
                    .flatMap(issued -> refreshTokens
                            .create(RefreshToken.issue(generator.hash(issued.getT2()), user.id(), now, refreshExpiresAt))
                            .thenReturn(new AuthTokens(
                                    issued.getT1().value(), issued.getT1().expiresAt(),
                                    issued.getT2(), refreshExpiresAt)));
        });
    }
}
