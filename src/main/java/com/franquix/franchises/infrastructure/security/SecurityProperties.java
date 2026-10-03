package com.franquix.franchises.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Authentication settings ({@code franquix.security.*}). Secrets come from the environment only.
 *
 * @param jwt            access token signing and lifetime
 * @param refreshTokenTtl lifetime of a refresh token
 * @param bootstrapAdmin admin account created at startup when it does not exist yet
 * @param rateLimit      request budgets per client
 */
@ConfigurationProperties("franquix.security")
public record SecurityProperties(
        Jwt jwt,
        @DefaultValue("1d") Duration refreshTokenTtl,
        BootstrapAdmin bootstrapAdmin,
        RateLimit rateLimit) {

    /** HS256 needs a key of at least 256 bits. */
    public static final int MIN_SECRET_BYTES = 32;

    public SecurityProperties {
        if (jwt == null || jwt.secret() == null || jwt.secret().getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "franquix.security.jwt.secret (JWT_SECRET) must be set and at least %d bytes long"
                            .formatted(MIN_SECRET_BYTES));
        }
        if (bootstrapAdmin == null) {
            bootstrapAdmin = new BootstrapAdmin(null, "Administrator", null);
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(5, 100, 1000);
        }
    }

    public record Jwt(
            String secret,
            @DefaultValue("franquix") String issuer,
            @DefaultValue("5m") Duration accessTokenTtl) {

        @Override
        public String toString() {
            return "Jwt[secret=****, issuer=%s, accessTokenTtl=%s]".formatted(issuer, accessTokenTtl);
        }
    }

    public record BootstrapAdmin(String email, @DefaultValue("Administrator") String name, String password) {

        public boolean configured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }

        @Override
        public String toString() {
            return "BootstrapAdmin[email=%s, name=%s, password=****]".formatted(email, name);
        }
    }

    /**
     * @param authPerMinute    login, refresh and password change attempts per client IP and minute
     * @param anonymousPerHour other unauthenticated requests per client IP and hour
     * @param userPerHour      authenticated requests per user and hour
     */
    public record RateLimit(
            @DefaultValue("5") int authPerMinute,
            @DefaultValue("100") int anonymousPerHour,
            @DefaultValue("1000") int userPerHour) {
    }
}
