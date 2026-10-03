package com.franquix.franchises.domain.port.out;

import com.franquix.franchises.domain.model.auth.User;
import java.time.Instant;
import reactor.core.publisher.Mono;

/** Persistence port for users. */
public interface UserRepositoryPort {

    /**
     * Inserts a new user.
     *
     * @return the stored user; errors with
     *     {@link com.franquix.franchises.domain.exception.DuplicateNameException} when the email is taken
     */
    Mono<User> create(User user);

    /** @param email already normalized (trimmed, lower-cased) */
    Mono<User> findByEmail(String email);

    Mono<User> findById(String userId);

    /** @return the updated user, or empty when it does not exist */
    Mono<User> updatePasswordHash(String userId, String passwordHash, Instant updatedAt);
}
