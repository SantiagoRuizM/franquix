package com.franquix.franchises.infrastructure.security;

import com.franquix.franchises.domain.port.out.PasswordHasherPort;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * BCrypt with cost 12. Hashing deliberately takes a few hundred milliseconds of CPU, so it always
 * runs on the bounded elastic scheduler, never on an event-loop thread.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

    static final int STRENGTH = 12;

    private final BCryptPasswordEncoder encoder;
    private final String unknownUserHash;

    public BCryptPasswordHasher() {
        this(STRENGTH);
    }

    BCryptPasswordHasher(int strength) {
        this.encoder = new BCryptPasswordEncoder(strength);
        // computed once at startup (main thread); matches no password anybody can know
        this.unknownUserHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public Mono<String> hash(String rawPassword) {
        return Mono.fromCallable(() -> encoder.encode(rawPassword)).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> matches(String rawPassword, String passwordHash) {
        return Mono.fromCallable(() -> encoder.matches(rawPassword, passwordHash))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String unknownUserHash() {
        return unknownUserHash;
    }
}
