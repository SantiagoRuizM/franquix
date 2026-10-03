package com.franquix.franchises.domain.port.in;

import reactor.core.publisher.Mono;

/** Changes the current user's password and revokes all their refresh tokens. */
@FunctionalInterface
public interface ChangePasswordUseCase {

    Mono<Void> changePassword(String userId, String currentPassword, String newPassword);
}
