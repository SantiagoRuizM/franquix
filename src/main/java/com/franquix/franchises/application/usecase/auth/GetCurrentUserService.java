package com.franquix.franchises.application.usecase.auth;

import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.in.GetCurrentUserUseCase;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import reactor.core.publisher.Mono;

public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepositoryPort users;

    public GetCurrentUserService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public Mono<User> get(String userId) {
        return users.findById(userId)
                .switchIfEmpty(Mono.error(AuthenticationFailedException::invalidCredentials));
    }
}
