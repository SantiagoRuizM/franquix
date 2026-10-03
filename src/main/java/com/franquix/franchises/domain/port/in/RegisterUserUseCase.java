package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import reactor.core.publisher.Mono;

/** Creates a user account (admin operation). */
@FunctionalInterface
public interface RegisterUserUseCase {

    Mono<User> register(String email, String name, String password, Role role);
}
