package com.franquix.franchises.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.infrastructure.security.ratelimit.FixedWindowRateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

class SecurityAdaptersTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-0123456789";

    private static SecurityProperties properties(String secret, Duration accessTtl) {
        return new SecurityProperties(
                new SecurityProperties.Jwt(secret, "franquix", accessTtl), Duration.ofDays(1), null, null);
    }

    @Nested
    class Properties {

        @Test
        void rejectsAMissingOrShortSecret() {
            assertThatThrownBy(() -> properties(null, Duration.ofMinutes(5)))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> properties("too-short", Duration.ofMinutes(5)))
                    .hasMessageContaining("at least 32 bytes");
        }

        @Test
        void appliesDefaultsAndNeverPrintsSecrets() {
            SecurityProperties props = properties(SECRET, Duration.ofMinutes(5));

            assertThat(props.rateLimit()).isEqualTo(new SecurityProperties.RateLimit(5, 100, 1000));
            assertThat(props.bootstrapAdmin().configured()).isFalse();
            assertThat(props.toString()).doesNotContain(SECRET);
            assertThat(new SecurityProperties.BootstrapAdmin("a@b.co", "A", "s3cret-value").toString())
                    .doesNotContain("s3cret-value");
        }
    }

    @Nested
    class RateLimiter {

        private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock);

        @Test
        void allowsUpToTheLimitThenRejectsWithRetryAfter() {
            for (int i = 0; i < 3; i++) {
                assertThat(limiter.tryAcquire("ip:1", 3, Duration.ofMinutes(1)).allowed()).isTrue();
            }
            clock.advance(Duration.ofSeconds(20));

            var rejected = limiter.tryAcquire("ip:1", 3, Duration.ofMinutes(1));
            assertThat(rejected.allowed()).isFalse();
            assertThat(rejected.retryAfterSeconds()).isEqualTo(40);
        }

        @Test
        void budgetsAreIndependentPerKey() {
            assertThat(limiter.tryAcquire("ip:1", 1, Duration.ofMinutes(1)).allowed()).isTrue();
            assertThat(limiter.tryAcquire("ip:2", 1, Duration.ofMinutes(1)).allowed()).isTrue();
            assertThat(limiter.tryAcquire("ip:1", 1, Duration.ofMinutes(1)).allowed()).isFalse();
        }

        @Test
        void aNewWindowStartsAfterTheOldOneEnds() {
            limiter.tryAcquire("ip:1", 1, Duration.ofMinutes(1));
            clock.advance(Duration.ofMinutes(1));

            assertThat(limiter.tryAcquire("ip:1", 1, Duration.ofMinutes(1)).allowed()).isTrue();
        }
    }

    @Nested
    class RefreshTokens {

        private final SecureRefreshTokenGenerator generator = new SecureRefreshTokenGenerator();

        @Test
        void generatesUniqueUrlSafeTokensWith256Bits() {
            Set<String> tokens = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                tokens.add(generator.generate().block());
            }

            assertThat(tokens).hasSize(50).allMatch(token -> token.matches("[A-Za-z0-9_-]{43}"));
        }

        @Test
        void hashesDeterministicallyWithSha256() {
            assertThat(generator.hash("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                    .isEqualTo(generator.hash("abc"));
        }
    }

    @Nested
    class Passwords {

        // the lowest BCrypt cost keeps the test fast; production uses 12
        private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(4);

        @Test
        void hashesWithASaltAndVerifies() {
            String first = hasher.hash("correct horse battery").block();
            String second = hasher.hash("correct horse battery").block();

            assertThat(first).startsWith("$2a$04$").isNotEqualTo(second);
            StepVerifier.create(hasher.matches("correct horse battery", first)).expectNext(true).verifyComplete();
            StepVerifier.create(hasher.matches("wrong", first)).expectNext(false).verifyComplete();
        }

        @Test
        void theUnknownUserHashMatchesNothingGuessable() {
            StepVerifier.create(hasher.matches("", hasher.unknownUserHash())).expectNext(false).verifyComplete();
            assertThat(new BCryptPasswordHasher().unknownUserHash()).startsWith("$2a$12$");
        }
    }

    @Nested
    class AccessTokens {

        private final Instant now = Instant.parse("2026-03-15T12:00:00Z");
        private final SecurityConfig config = new SecurityConfig();

        private ReactiveJwtDecoder decoder(SecurityProperties props) {
            return config.jwtDecoder(config.jwtSigningKey(props), props);
        }

        @Test
        void issuedTokensVerifyAndCarryIdentityAndRole() {
            SecurityProperties props = properties(SECRET, Duration.ofMinutes(5));
            var issuer = new JwtAccessTokenIssuer(config.jwtEncoder(config.jwtSigningKey(props)), props,
                    Clock.systemUTC());
            User user = new User("6650f1c2a1b2c3d4e5f60720", "admin@franquix.local", "Admin", "hash", Role.ADMIN,
                    true, now, now);

            var token = issuer.issue(user).block();

            StepVerifier.create(decoder(props).decode(token.value()))
                    .assertNext(jwt -> {
                        assertThat(jwt.getSubject()).isEqualTo(user.id());
                        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
                        assertThat(jwt.getClaimAsString("email")).isEqualTo("admin@franquix.local");
                        assertThat(jwt.getId()).isNotBlank();
                        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
                    })
                    .verifyComplete();
        }

        @Test
        void tokensSignedWithAnotherSecretAreRejected() {
            SecurityProperties attacker = properties("another-secret-that-is-long-enough-987654321", Duration.ofMinutes(5));
            var forged = new JwtAccessTokenIssuer(config.jwtEncoder(config.jwtSigningKey(attacker)), attacker,
                    Clock.systemUTC()).issue(sampleUser()).block();

            StepVerifier.create(decoder(properties(SECRET, Duration.ofMinutes(5))).decode(forged.value()))
                    .expectError()
                    .verify();
        }

        @Test
        void expiredTokensAreRejected() {
            SecurityProperties props = properties(SECRET, Duration.ofMinutes(5));
            Clock longAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
            var expired = new JwtAccessTokenIssuer(config.jwtEncoder(config.jwtSigningKey(props)), props, longAgo)
                    .issue(sampleUser()).block();

            StepVerifier.create(decoder(props).decode(expired.value()))
                    .expectError(JwtValidationException.class)
                    .verify();
        }

        private User sampleUser() {
            return new User("6650f1c2a1b2c3d4e5f60720", "admin@franquix.local", "Admin", "hash", Role.ADMIN,
                    true, now, now);
        }
    }

    /** A clock tests can move forward. */
    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> now;

        MutableClock(Instant start) {
            this.now = new AtomicReference<>(start);
        }

        void advance(Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public Instant instant() {
            return now.get();
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
