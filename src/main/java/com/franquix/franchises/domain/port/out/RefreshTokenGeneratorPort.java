package com.franquix.franchises.domain.port.out;

import reactor.core.publisher.Mono;

/** Creates unguessable opaque refresh tokens and the one-way hash stored for them. */
public interface RefreshTokenGeneratorPort {

    Mono<String> generate();

    String hash(String token);
}
