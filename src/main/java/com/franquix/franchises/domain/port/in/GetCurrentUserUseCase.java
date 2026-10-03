package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.auth.User;
import reactor.core.publisher.Mono;

/** Returns the profile of the authenticated user. */
@FunctionalInterface
public interface GetCurrentUserUseCase {

    Mono<User> get(String userId);
}
