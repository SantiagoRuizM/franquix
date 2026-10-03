package com.franquix.franchises.domain.port.out;

import reactor.core.publisher.Mono;

/** One-way password hashing. Implementations must keep the slow hashing off event-loop threads. */
public interface PasswordHasherPort {

    Mono<String> hash(String rawPassword);

    Mono<Boolean> matches(String rawPassword, String passwordHash);

    /**
     * A valid hash that matches no real password. Checking against it when the email is unknown makes
     * a failed login take as long as a wrong password, so response times do not reveal which emails exist.
     */
    String unknownUserHash();
}
