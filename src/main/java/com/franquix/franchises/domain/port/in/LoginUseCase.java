package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.auth.AuthTokens;
import reactor.core.publisher.Mono;

/** Exchanges email and password for an access token and a refresh token. */
@FunctionalInterface
public interface LoginUseCase {

    Mono<AuthTokens> login(String email, String password);
}
