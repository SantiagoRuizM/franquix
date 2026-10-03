package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.model.auth.AuthTokens;
import com.franquix.franchises.domain.model.auth.Role;
import com.franquix.franchises.domain.model.auth.User;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

class AuthHandlerTest extends RestTestSupport {

    private static final AuthTokens TOKENS = new AuthTokens(
            "access.jwt", NOW.plus(Duration.ofMinutes(5)), "opaque-refresh", NOW.plus(Duration.ofDays(1)));
    private static final User ADMIN = new User(
            USER_ID, "admin@franquix.local", "Administrator", "$2a$hash", Role.ADMIN, true, NOW, NOW);

    @Test
    void loginReturnsATokenPairThatIsNeverCached() {
        when(login.login("admin@franquix.local", "a-long-password")).thenReturn(Mono.just(TOKENS));

        anonymous.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "admin@franquix.local", "password", "a-long-password"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(HttpHeaders.CACHE_CONTROL, value -> assertThat(value).contains("no-store"))
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access.jwt")
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isEqualTo(300)
                .jsonPath("$.refreshToken").isEqualTo("opaque-refresh")
                .jsonPath("$.refreshExpiresIn").isEqualTo(86400);
    }

    @Test
    void failedLoginIsA401WithAGenericMessage() {
        when(login.login("admin@franquix.local", "wrong-password"))
                .thenReturn(Mono.error(AuthenticationFailedException.invalidCredentials()));

        anonymous.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "admin@franquix.local", "password", "wrong-password"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody().jsonPath("$.detail").isEqualTo("Invalid email or password");
    }

    @Test
    void loginRequiresEmailAndPassword() {
        anonymous.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", " "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.email").isEqualTo("must not be blank")
                .jsonPath("$.errors.password").isEqualTo("must not be blank");
        verifyNoInteractions(login);
    }

    @Test
    void refreshReturnsANewPair() {
        when(refreshSession.refresh("opaque-refresh")).thenReturn(Mono.just(TOKENS));

        anonymous.post().uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", "opaque-refresh"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.accessToken").isEqualTo("access.jwt");
    }

    @Test
    void refreshWithAnInvalidTokenIs401() {
        when(refreshSession.refresh("stale"))
                .thenReturn(Mono.error(AuthenticationFailedException.invalidRefreshToken()));

        anonymous.post().uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", "stale"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("Invalid or expired refresh token");
    }

    @Test
    void logoutRevokesTheTokenOfTheAuthenticatedUser() {
        when(logout.logout(USER_ID, "opaque-refresh")).thenReturn(Mono.empty());

        client.post().uri("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", "opaque-refresh"))
                .exchange()
                .expectStatus().isNoContent();
        verify(logout).logout(USER_ID, "opaque-refresh");
    }

    @Test
    void meReturnsTheProfileWithoutThePasswordHash() {
        when(currentUser.get(USER_ID)).thenReturn(Mono.just(ADMIN));

        client.get().uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(USER_ID)
                .jsonPath("$.email").isEqualTo("admin@franquix.local")
                .jsonPath("$.role").isEqualTo("ADMIN")
                .jsonPath("$.passwordHash").doesNotExist();
    }

    @Test
    void changePasswordReturns204() {
        when(changePassword.changePassword(USER_ID, "old-password-123", "new-password-456"))
                .thenReturn(Mono.empty());

        as(Role.VIEWER).patch().uri("/api/v1/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("currentPassword", "old-password-123", "newPassword", "new-password-456"))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void changePasswordReportsPolicyViolations() {
        when(changePassword.changePassword(USER_ID, "old-password-123", "123456789012"))
                .thenReturn(Mono.error(new DomainValidationException("password must not be entirely numeric")));

        client.patch().uri("/api/v1/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("currentPassword", "old-password-123", "newPassword", "123456789012"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("password must not be entirely numeric");
    }

    @Test
    void adminsCreateUsers() {
        User viewer = new User("6650f1c2a1b2c3d4e5f60721", "viewer@franquix.local", "Viewer", "$2a$hash",
                Role.VIEWER, true, NOW, NOW);
        when(registerUser.register("viewer@franquix.local", "Viewer", "a-strong-passphrase", Role.VIEWER))
                .thenReturn(Mono.just(viewer));

        client.post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "viewer@franquix.local", "name", "Viewer",
                        "password", "a-strong-passphrase", "role", "VIEWER"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/v1/users/6650f1c2a1b2c3d4e5f60721")
                .expectBody()
                .jsonPath("$.role").isEqualTo("VIEWER")
                .jsonPath("$.passwordHash").doesNotExist();
    }

    @Test
    void creatingAUserWithAnUnknownRoleIs400() {
        client.post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "x@franquix.local", "name", "X",
                        "password", "a-strong-passphrase", "role", "ROOT"))
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(registerUser);
    }

    @Test
    void duplicateEmailsAre409() {
        when(registerUser.register("viewer@franquix.local", "Viewer", "a-strong-passphrase", Role.VIEWER))
                .thenReturn(Mono.error(DuplicateNameException.email("viewer@franquix.local")));

        client.post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "viewer@franquix.local", "name", "Viewer",
                        "password", "a-strong-passphrase", "role", "VIEWER"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }
}
