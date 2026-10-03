package com.franquix.franchises.infrastructure.security;

import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.in.RegisterUserUseCase;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Creates the first ADMIN from {@code ADMIN_EMAIL} / {@code ADMIN_PASSWORD} when that account does not
 * exist yet. It never changes an existing account and never logs the password. Runs once on the main
 * thread after the indexes exist (the unique email index guards against duplicates).
 */
@Component
@DependsOn("mongoIndexInitializer")
public class AdminBootstrap implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final SecurityProperties.BootstrapAdmin admin;
    private final UserRepositoryPort users;
    private final RegisterUserUseCase registerUser;

    public AdminBootstrap(SecurityProperties properties, UserRepositoryPort users, RegisterUserUseCase registerUser) {
        this.admin = properties.bootstrapAdmin();
        this.users = users;
        this.registerUser = registerUser;
    }

    @Override
    public void afterPropertiesSet() {
        ensureAdmin().block(TIMEOUT);
    }

    Mono<Void> ensureAdmin() {
        if (!admin.configured()) {
            log.warn("No bootstrap admin configured (ADMIN_EMAIL / ADMIN_PASSWORD); nobody can log in "
                    + "until an account exists");
            return Mono.empty();
        }
        return Mono.fromCallable(() -> User.normalizeEmail(admin.email()))
                .flatMap(email -> users.findByEmail(email)
                        .doOnNext(existing -> log.info("Bootstrap admin {} already exists", email))
                        .switchIfEmpty(Mono.defer(() -> registerUser
                                .register(email, admin.name(), admin.password(), Role.ADMIN)
                                .doOnNext(created -> log.info("Bootstrap admin {} created", email)))))
                .then();
    }
}
