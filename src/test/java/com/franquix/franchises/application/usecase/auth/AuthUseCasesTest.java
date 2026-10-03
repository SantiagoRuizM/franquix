package com.franquix.franchises.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.auth.AccessToken;
import com.franquix.franchises.domain.model.auth.AuthTokens;
import com.franquix.franchises.domain.model.auth.RefreshToken;
import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import com.franquix.franchises.domain.port.out.AccessTokenIssuerPort;
import com.franquix.franchises.domain.port.out.PasswordHasherPort;
import com.franquix.franchises.domain.port.out.RefreshTokenGeneratorPort;
import com.franquix.franchises.domain.port.out.RefreshTokenRepositoryPort;
import com.franquix.franchises.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-15T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration REFRESH_TTL = Duration.ofDays(1);
    private static final String USER_ID = "6650f1c2a1b2c3d4e5f60720";
    private static final String EMAIL = "admin@franquix.local";
    private static final String PASSWORD = "correct horse battery";
    private static final User ACTIVE = new User(USER_ID, EMAIL, "Admin", "stored-hash", Role.ADMIN, true, NOW, NOW);
    private static final User INACTIVE = new User(USER_ID, EMAIL, "Admin", "stored-hash", Role.ADMIN, false, NOW, NOW);
    private static final AccessToken ACCESS = new AccessToken("access.jwt", NOW.plusSeconds(300));

    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private AccessTokenIssuerPort accessTokens;
    @Mock
    private RefreshTokenGeneratorPort generator;
    @Mock
    private RefreshTokenRepositoryPort refreshTokens;

    private SessionIssuer sessions;

    @BeforeEach
    void setUp() {
        sessions = new SessionIssuer(accessTokens, generator, refreshTokens, REFRESH_TTL, CLOCK);
        when(accessTokens.issue(any())).thenReturn(Mono.just(ACCESS));
        when(generator.generate()).thenReturn(Mono.just("new-refresh"));
        when(generator.hash(anyString())).thenAnswer(call -> "hash(" + call.getArgument(0) + ")");
        when(refreshTokens.create(any())).thenAnswer(call -> Mono.just(call.getArgument(0)));
        when(hasher.unknownUserHash()).thenReturn("dummy-hash");
    }

    private static AuthTokens expectedTokens() {
        return new AuthTokens("access.jwt", NOW.plusSeconds(300), "new-refresh", NOW.plus(REFRESH_TTL));
    }

    @Nested
    class Login {

        private LoginService useCase;

        @BeforeEach
        void setUp() {
            useCase = new LoginService(users, hasher, sessions);
        }

        @Test
        void opensASessionAndStoresOnlyTheRefreshTokenHash() {
            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(ACTIVE));
            when(hasher.matches(PASSWORD, "stored-hash")).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.login("  Admin@Franquix.local ", PASSWORD))
                    .expectNext(expectedTokens())
                    .verifyComplete();

            ArgumentCaptor<RefreshToken> stored = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokens).create(stored.capture());
            assertThat(stored.getValue())
                    .isEqualTo(RefreshToken.issue("hash(new-refresh)", USER_ID, NOW, NOW.plus(REFRESH_TTL)));
        }

        @Test
        void rejectsAWrongPassword() {
            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(ACTIVE));
            when(hasher.matches("wrong", "stored-hash")).thenReturn(Mono.just(false));

            StepVerifier.create(useCase.login(EMAIL, "wrong"))
                    .expectErrorMessage("Invalid email or password")
                    .verify();
            verify(accessTokens, never()).issue(any());
        }

        @Test
        void checksAPasswordEvenForUnknownEmailsSoTimingDoesNotLeak() {
            when(users.findByEmail("ghost@franquix.local")).thenReturn(Mono.empty());
            when(hasher.matches(PASSWORD, "dummy-hash")).thenReturn(Mono.just(false));

            StepVerifier.create(useCase.login("ghost@franquix.local", PASSWORD))
                    .expectError(AuthenticationFailedException.class)
                    .verify();
            verify(hasher).matches(PASSWORD, "dummy-hash");
        }

        @Test
        void rejectsInactiveUsersWithTheSameError() {
            when(users.findByEmail(EMAIL)).thenReturn(Mono.just(INACTIVE));
            when(hasher.matches(PASSWORD, "stored-hash")).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.login(EMAIL, PASSWORD))
                    .expectErrorMessage("Invalid email or password")
                    .verify();
        }

        @Test
        void treatsAMalformedEmailAsUnknown() {
            when(hasher.matches(PASSWORD, "dummy-hash")).thenReturn(Mono.just(false));

            StepVerifier.create(useCase.login("not-an-email", PASSWORD))
                    .expectErrorMessage("Invalid email or password")
                    .verify();
            verify(users, never()).findByEmail(any());
        }
    }

    @Nested
    class Refresh {

        private RefreshSessionService useCase;

        @BeforeEach
        void setUp() {
            useCase = new RefreshSessionService(refreshTokens, generator, users, sessions, CLOCK);
        }

        @Test
        void rotatesAUsableToken() {
            when(refreshTokens.consume("hash(old)", NOW))
                    .thenReturn(Mono.just(RefreshToken.issue("hash(old)", USER_ID, NOW, NOW.plus(REFRESH_TTL))));
            when(users.findById(USER_ID)).thenReturn(Mono.just(ACTIVE));

            StepVerifier.create(useCase.refresh("old"))
                    .expectNext(expectedTokens())
                    .verifyComplete();
            verify(refreshTokens, never()).revokeAllForUser(any(), any());
        }

        @Test
        void rejectsUnknownOrExpiredTokens() {
            when(refreshTokens.consume("hash(unknown)", NOW)).thenReturn(Mono.empty());
            when(refreshTokens.findByHash("hash(unknown)")).thenReturn(Mono.empty());

            StepVerifier.create(useCase.refresh("unknown"))
                    .expectErrorMessage("Invalid or expired refresh token")
                    .verify();
            verify(refreshTokens, never()).revokeAllForUser(any(), any());
        }

        @Test
        void revokesEverySessionWhenARotatedTokenIsReused() {
            when(refreshTokens.consume("hash(stolen)", NOW)).thenReturn(Mono.empty());
            when(refreshTokens.findByHash("hash(stolen)")).thenReturn(Mono.just(
                    new RefreshToken("hash(stolen)", USER_ID, NOW, NOW.plus(REFRESH_TTL), NOW)));
            when(refreshTokens.revokeAllForUser(USER_ID, NOW)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.refresh("stolen"))
                    .expectError(AuthenticationFailedException.class)
                    .verify();
            verify(refreshTokens).revokeAllForUser(USER_ID, NOW);
        }

        @Test
        void rejectsTokensOfDeactivatedUsers() {
            when(refreshTokens.consume("hash(old)", NOW))
                    .thenReturn(Mono.just(RefreshToken.issue("hash(old)", USER_ID, NOW, NOW.plus(REFRESH_TTL))));
            when(users.findById(USER_ID)).thenReturn(Mono.just(INACTIVE));
            when(refreshTokens.findByHash("hash(old)")).thenReturn(Mono.just(
                    new RefreshToken("hash(old)", USER_ID, NOW, NOW.plus(REFRESH_TTL), NOW)));
            when(refreshTokens.revokeAllForUser(USER_ID, NOW)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.refresh("old"))
                    .expectError(AuthenticationFailedException.class)
                    .verify();
            verify(accessTokens, never()).issue(any());
        }
    }

    @Test
    void logoutRevokesTheHashedTokenOfTheUser() {
        when(refreshTokens.revoke("hash(token)", USER_ID, NOW)).thenReturn(Mono.empty());

        StepVerifier.create(new LogoutService(refreshTokens, generator, CLOCK).logout(USER_ID, "token"))
                .verifyComplete();
        verify(refreshTokens).revoke("hash(token)", USER_ID, NOW);
    }

    @Nested
    class ChangePassword {

        private static final String NEW_PASSWORD = "a brand new passphrase";

        private ChangePasswordService useCase;

        @BeforeEach
        void setUp() {
            useCase = new ChangePasswordService(users, hasher, refreshTokens, CLOCK);
        }

        @Test
        void storesTheNewHashAndEndsEverySession() {
            when(users.findById(USER_ID)).thenReturn(Mono.just(ACTIVE));
            when(hasher.matches(PASSWORD, "stored-hash")).thenReturn(Mono.just(true));
            when(hasher.hash(NEW_PASSWORD)).thenReturn(Mono.just("new-hash"));
            when(users.updatePasswordHash(USER_ID, "new-hash", NOW)).thenReturn(Mono.just(ACTIVE));
            when(refreshTokens.revokeAllForUser(USER_ID, NOW)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.changePassword(USER_ID, PASSWORD, NEW_PASSWORD))
                    .verifyComplete();
            verify(refreshTokens).revokeAllForUser(USER_ID, NOW);
        }

        @Test
        void requiresTheCurrentPassword() {
            when(users.findById(USER_ID)).thenReturn(Mono.just(ACTIVE));
            when(hasher.matches("wrong", "stored-hash")).thenReturn(Mono.just(false));

            StepVerifier.create(useCase.changePassword(USER_ID, "wrong", NEW_PASSWORD))
                    .expectErrorMessage("current password is incorrect")
                    .verify();
            verify(users, never()).updatePasswordHash(any(), any(), any());
        }

        @Test
        void appliesThePasswordPolicy() {
            when(users.findById(USER_ID)).thenReturn(Mono.just(ACTIVE));
            when(hasher.matches(PASSWORD, "stored-hash")).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.changePassword(USER_ID, PASSWORD, "short"))
                    .expectError(DomainValidationException.class)
                    .verify();
            verify(hasher, never()).hash(any());
        }

        @Test
        void rejectsReusingTheCurrentPassword() {
            when(users.findById(USER_ID)).thenReturn(Mono.just(ACTIVE));
            when(hasher.matches(PASSWORD, "stored-hash")).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.changePassword(USER_ID, PASSWORD, PASSWORD))
                    .expectErrorMessage("new password must differ from the current one")
                    .verify();
        }

        @Test
        void rejectsUnknownOrInactiveUsers() {
            when(users.findById(USER_ID)).thenReturn(Mono.just(INACTIVE));

            StepVerifier.create(useCase.changePassword(USER_ID, PASSWORD, NEW_PASSWORD))
                    .expectError(AuthenticationFailedException.class)
                    .verify();
        }
    }

    @Nested
    class Register {

        private RegisterUserService useCase;

        @BeforeEach
        void setUp() {
            useCase = new RegisterUserService(users, hasher, CLOCK);
        }

        @Test
        void hashesThePasswordAndStoresANormalizedUser() {
            User expected = new User(null, "viewer@franquix.local", "Viewer", "hashed", Role.VIEWER, true, NOW, NOW);
            when(hasher.hash(PASSWORD)).thenReturn(Mono.just("hashed"));
            when(users.create(expected)).thenReturn(Mono.just(expected));

            StepVerifier.create(useCase.register(" Viewer@Franquix.local", "Viewer", PASSWORD, Role.VIEWER))
                    .expectNext(expected)
                    .verifyComplete();
        }

        @Test
        void rejectsWeakPasswordsBeforeHashing() {
            StepVerifier.create(useCase.register("viewer@franquix.local", "Viewer", "123456789012", Role.VIEWER))
                    .expectErrorMessage("password must not be entirely numeric")
                    .verify();
            verify(hasher, never()).hash(any());
        }

        @Test
        void propagatesDuplicateEmails() {
            when(hasher.hash(PASSWORD)).thenReturn(Mono.just("hashed"));
            when(users.create(any())).thenReturn(Mono.error(DuplicateNameException.email("viewer@franquix.local")));

            StepVerifier.create(useCase.register("viewer@franquix.local", "Viewer", PASSWORD, Role.VIEWER))
                    .expectError(DuplicateNameException.class)
                    .verify();
        }
    }

    @Test
    void currentUserFailsWhenTheAccountNoLongerExists() {
        when(users.findById(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(new GetCurrentUserService(users).get(USER_ID))
                .expectError(AuthenticationFailedException.class)
                .verify();
    }

    @Test
    void currentUserReturnsTheAccount() {
        when(users.findById(USER_ID)).thenReturn(Mono.just(ACTIVE));

        StepVerifier.create(new GetCurrentUserService(users).get(USER_ID))
                .expectNext(ACTIVE)
                .verifyComplete();
    }
}
