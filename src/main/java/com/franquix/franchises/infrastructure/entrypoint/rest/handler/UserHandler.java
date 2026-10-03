package com.franquix.franchises.infrastructure.entrypoint.rest.handler;

import com.franquix.franchises.domain.port.in.RegisterUserUseCase;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.CreateUserRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.mapper.RestMapper;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class UserHandler {

    private final RegisterUserUseCase registerUser;
    private final RequestValidator validator;

    public UserHandler(RegisterUserUseCase registerUser, RequestValidator validator) {
        this.registerUser = registerUser;
        this.validator = validator;
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return validator.body(request, CreateUserRequest.class)
                .flatMap(body -> registerUser.register(body.email(), body.name(), body.password(), body.role()))
                .flatMap(user -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(user.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(RestMapper.toResponse(user)));
    }
}
