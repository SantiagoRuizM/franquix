package com.franquix.franchises.infrastructure.config;

import com.franquix.franchises.application.usecase.auth.ChangePasswordService;
import com.franquix.franchises.application.usecase.auth.GetCurrentUserService;
import com.franquix.franchises.application.usecase.auth.LoginService;
import com.franquix.franchises.application.usecase.auth.LogoutService;
import com.franquix.franchises.application.usecase.auth.RefreshSessionService;
import com.franquix.franchises.application.usecase.auth.RegisterUserService;
import com.franquix.franchises.application.usecase.auth.SessionIssuer;
import com.franquix.franchises.domain.port.in.ChangePasswordUseCase;
import com.franquix.franchises.domain.port.in.GetCurrentUserUseCase;
import com.franquix.franchises.domain.port.in.LoginUseCase;
import com.franquix.franchises.domain.port.in.LogoutUseCase;
import com.franquix.franchises.domain.port.in.RefreshSessionUseCase;
import com.franquix.franchises.domain.port.in.RegisterUserUseCase;
import com.franquix.franchises.domain.port.out.AccessTokenIssuerPort;
import com.franquix.franchises.domain.port.out.PasswordHasherPort;
import com.franquix.franchises.domain.port.out.RefreshTokenGeneratorPort;
import com.franquix.franchises.domain.port.out.RefreshTokenRepositoryPort;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import com.franquix.franchises.infrastructure.security.SecurityProperties;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the framework-free authentication use cases as Spring beans. */
@Configuration(proxyBeanMethods = false)
public class AuthConfig {

    @Bean
    SessionIssuer sessionIssuer(
            AccessTokenIssuerPort accessTokens,
            RefreshTokenGeneratorPort generator,
            RefreshTokenRepositoryPort refreshTokens,
            SecurityProperties properties,
            Clock clock) {
        return new SessionIssuer(accessTokens, generator, refreshTokens, properties.refreshTokenTtl(), clock);
    }

    @Bean
    LoginUseCase loginUseCase(UserRepositoryPort users, PasswordHasherPort hasher, SessionIssuer sessions) {
        return new LoginService(users, hasher, sessions);
    }

    @Bean
    RefreshSessionUseCase refreshSessionUseCase(
            RefreshTokenRepositoryPort refreshTokens,
            RefreshTokenGeneratorPort generator,
            UserRepositoryPort users,
            SessionIssuer sessions,
            Clock clock) {
        return new RefreshSessionService(refreshTokens, generator, users, sessions, clock);
    }

    @Bean
    LogoutUseCase logoutUseCase(RefreshTokenRepositoryPort refreshTokens, RefreshTokenGeneratorPort generator, Clock clock) {
        return new LogoutService(refreshTokens, generator, clock);
    }

    @Bean
    ChangePasswordUseCase changePasswordUseCase(
            UserRepositoryPort users, PasswordHasherPort hasher, RefreshTokenRepositoryPort refreshTokens, Clock clock) {
        return new ChangePasswordService(users, hasher, refreshTokens, clock);
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepositoryPort users, PasswordHasherPort hasher, Clock clock) {
        return new RegisterUserService(users, hasher, clock);
    }

    @Bean
    GetCurrentUserUseCase getCurrentUserUseCase(UserRepositoryPort users) {
        return new GetCurrentUserService(users);
    }
}
