package com.franquix.franchises.application.usecase.auth;

import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.model.auth.PasswordPolicy;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.in.ChangePasswordUseCase;
import com.franquix.franchises.domain.port.out.PasswordHasherPort;
import com.franquix.franchises.domain.port.out.RefreshTokenRepositoryPort;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

/** Requires the current password, applies the password policy and ends every existing session. */
public class ChangePasswordService implements ChangePasswordUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final Clock clock;

    public ChangePasswordService(
            UserRepositoryPort users, PasswordHasherPort hasher, RefreshTokenRepositoryPort refreshTokens, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Override
    public Mono<Void> changePassword(String userId, String currentPassword, String newPassword) {
        return users.findById(userId)
                .filter(User::active)
                .switchIfEmpty(Mono.error(AuthenticationFailedException::invalidCredentials))
                .flatMap(user -> hasher.matches(currentPassword, user.passwordHash())
                        .flatMap(matches -> matches
                                ? Mono.just(user)
                                : Mono.error(new DomainValidationException("current password is incorrect"))))
                .flatMap(user -> Mono.fromRunnable(() -> validateNewPassword(user, currentPassword, newPassword))
                        .then(Mono.defer(() -> hasher.hash(newPassword)))
                        .flatMap(hash -> users.updatePasswordHash(user.id(), hash, clock.instant())))
                .flatMap(updated -> refreshTokens.revokeAllForUser(updated.id(), clock.instant()));
    }

    private static void validateNewPassword(User user, String currentPassword, String newPassword) {
        PasswordPolicy.validate(newPassword, user.email());
        if (newPassword.equals(currentPassword)) {
            throw new DomainValidationException("new password must differ from the current one");
        }
    }
}
