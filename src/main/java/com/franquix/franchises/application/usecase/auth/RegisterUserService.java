package com.franquix.franchises.application.usecase.auth;

import com.franquix.franchises.domain.model.auth.PasswordPolicy;
import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.in.RegisterUserUseCase;
import com.franquix.franchises.domain.port.out.PasswordHasherPort;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final Clock clock;

    public RegisterUserService(UserRepositoryPort users, PasswordHasherPort hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    public Mono<User> register(String email, String name, String password, Role role) {
        return Mono.fromCallable(() -> {
                    String normalizedEmail = User.normalizeEmail(email);
                    PasswordPolicy.validate(password, normalizedEmail);
                    return normalizedEmail;
                })
                .flatMap(normalizedEmail -> hasher.hash(password)
                        .map(hash -> User.create(normalizedEmail, name, hash, role, clock.instant())))
                .flatMap(users::create);
    }
}
