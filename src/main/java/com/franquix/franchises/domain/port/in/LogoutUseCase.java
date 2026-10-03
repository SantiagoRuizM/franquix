package com.franquix.franchises.domain.port.in;

import reactor.core.publisher.Mono;

/** Revokes a refresh token of the current user. */
@FunctionalInterface
public interface LogoutUseCase {

    Mono<Void> logout(String userId, String refreshToken);
}
