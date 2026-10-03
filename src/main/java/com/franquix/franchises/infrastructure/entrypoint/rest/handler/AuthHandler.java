package com.franquix.franchises.infrastructure.entrypoint.rest.handler;

import com.franquix.franchises.domain.model.auth.AuthTokens;
import com.franquix.franchises.domain.port.in.ChangePasswordUseCase;
import com.franquix.franchises.domain.port.in.GetCurrentUserUseCase;
import com.franquix.franchises.domain.port.in.LoginUseCase;
import com.franquix.franchises.domain.port.in.LogoutUseCase;
import com.franquix.franchises.domain.port.in.RefreshSessionUseCase;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.ChangePasswordRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.LoginRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.RefreshTokenRequest;
import com.franquix.franchises.infrastructure.entrypoint.rest.dto.auth.TokenResponse;
import com.franquix.franchises.infrastructure.entrypoint.rest.mapper.RestMapper;
import com.franquix.franchises.infrastructure.entrypoint.rest.validation.RequestValidator;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class AuthHandler {

    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;
    private final ChangePasswordUseCase changePassword;
    private final GetCurrentUserUseCase currentUser;
    private final RequestValidator validator;
    private final Clock clock;

    public AuthHandler(
            LoginUseCase login,
            RefreshSessionUseCase refreshSession,
            LogoutUseCase logout,
            ChangePasswordUseCase changePassword,
            GetCurrentUserUseCase currentUser,
            RequestValidator validator,
            Clock clock) {
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
        this.changePassword = changePassword;
        this.currentUser = currentUser;
        this.validator = validator;
        this.clock = clock;
    }

    public Mono<ServerResponse> login(ServerRequest request) {
        return validator.body(request, LoginRequest.class)
                .flatMap(body -> login.login(body.email(), body.password()))
                .flatMap(this::tokens);
    }

    public Mono<ServerResponse> refresh(ServerRequest request) {
        return validator.body(request, RefreshTokenRequest.class)
                .flatMap(body -> refreshSession.refresh(body.refreshToken()))
                .flatMap(this::tokens);
    }

    public Mono<ServerResponse> logout(ServerRequest request) {
        return userId(request)
                .flatMap(userId -> validator.body(request, RefreshTokenRequest.class)
                        .flatMap(body -> logout.logout(userId, body.refreshToken())))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> me(ServerRequest request) {
        return userId(request)
                .flatMap(currentUser::get)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(RestMapper.toResponse(user)));
    }

    public Mono<ServerResponse> changePassword(ServerRequest request) {
        return userId(request)
                .flatMap(userId -> validator.body(request, ChangePasswordRequest.class)
                        .flatMap(body -> changePassword.changePassword(
                                userId, body.currentPassword(), body.newPassword())))
                .then(ServerResponse.noContent().build());
    }

    private static Mono<String> userId(ServerRequest request) {
        return request.principal().map(Principal::getName);
    }

    private Mono<ServerResponse> tokens(AuthTokens tokens) {
        Instant now = clock.instant();
        TokenResponse body = new TokenResponse(
                tokens.accessToken(), "Bearer", secondsUntil(now, tokens.accessTokenExpiresAt()),
                tokens.refreshToken(), secondsUntil(now, tokens.refreshTokenExpiresAt()));
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Cache-Control", "no-store")
                .bodyValue(body);
    }

    private static long secondsUntil(Instant now, Instant expiresAt) {
        return Math.max(0, Duration.between(now, expiresAt).toSeconds());
    }
}
