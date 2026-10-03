package com.franquix.franchises.application.usecase.auth;

import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.model.auth.AuthTokens;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.in.LoginUseCase;
import com.franquix.franchises.domain.port.out.PasswordHasherPort;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * Every failure (malformed or unknown email, wrong password, inactive account) produces the same
 * error after the same amount of hashing work, so the endpoint does not reveal which accounts exist.
 */
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final SessionIssuer sessions;

    public LoginService(UserRepositoryPort users, PasswordHasherPort hasher, SessionIssuer sessions) {
        this.users = users;
        this.hasher = hasher;
        this.sessions = sessions;
    }

    @Override
    public Mono<AuthTokens> login(String email, String password) {
        return Mono.fromCallable(() -> User.normalizeEmail(email))
                .onErrorResume(DomainValidationException.class, invalid -> Mono.empty())
                .flatMap(users::findByEmail)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(user -> verify(user, password))
                .flatMap(sessions::open)
                .switchIfEmpty(Mono.error(AuthenticationFailedException::invalidCredentials));
    }

    private Mono<User> verify(Optional<User> user, String password) {
        String hash = user.map(User::passwordHash).orElseGet(hasher::unknownUserHash);
        return hasher.matches(password == null ? "" : password, hash)
                .filter(matches -> matches && user.map(User::active).orElse(false))
                .map(matches -> user.orElseThrow());
    }
}
