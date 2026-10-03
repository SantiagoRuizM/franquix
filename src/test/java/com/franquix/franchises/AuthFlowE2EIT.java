package com.franquix.franchises;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.franquix.franchises.support.E2ESupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Authentication end to end: real BCrypt, JWT, refresh-token rotation and MongoDB. */
class AuthFlowE2EIT extends E2ESupport {

    private static final String PASSWORD = "viewer passphrase 2026";

    private String createViewer() {
        String email = "viewer-" + System.nanoTime() + "@franquix.local";
        client.post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "name", "Viewer", "password", PASSWORD, "role", "VIEWER"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.passwordHash").doesNotExist();
        return email;
    }

    private WebTestClient.ResponseSpec refresh(String refreshToken) {
        return http.post().uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", refreshToken))
                .exchange();
    }

    @Test
    void theBootstrapAdminCanSeeTheirProfile() {
        client.get().uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(ADMIN_EMAIL)
                .jsonPath("$.role").isEqualTo("ADMIN");
    }

    @Test
    void viewersReadButCannotWrite() {
        String email = createViewer();
        WebTestClient viewer = withToken(login(email, PASSWORD).get("accessToken").asText());

        viewer.get().uri("/api/v1/franchises").exchange().expectStatus().isOk();
        viewer.post().uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", unique("Nope")))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void refreshTokensRotateAndReuseRevokesTheWholeSession() {
        JsonNode first = login(createViewer(), PASSWORD);
        String original = first.get("refreshToken").asText();

        JsonNode second = refresh(original).expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(second).isNotNull();
        String rotated = second.get("refreshToken").asText();
        assertThat(rotated).isNotEqualTo(original);

        // the old token was single use: presenting it again is treated as theft...
        refresh(original).expectStatus().isUnauthorized();
        // ...so the rotated token is revoked too
        refresh(rotated).expectStatus().isUnauthorized();
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        String email = createViewer();
        JsonNode tokens = login(email, PASSWORD);
        String refreshToken = tokens.get("refreshToken").asText();

        withToken(tokens.get("accessToken").asText()).post().uri("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", refreshToken))
                .exchange()
                .expectStatus().isNoContent();

        refresh(refreshToken).expectStatus().isUnauthorized();
    }

    @Test
    void changingThePasswordEndsSessionsAndReplacesTheCredential() {
        String email = createViewer();
        JsonNode tokens = login(email, PASSWORD);
        String newPassword = "a completely new passphrase";

        withToken(tokens.get("accessToken").asText()).patch().uri("/api/v1/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("currentPassword", PASSWORD, "newPassword", newPassword))
                .exchange()
                .expectStatus().isNoContent();

        refresh(tokens.get("refreshToken").asText()).expectStatus().isUnauthorized();
        http.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", PASSWORD))
                .exchange()
                .expectStatus().isUnauthorized();
        login(email, newPassword);
    }

    @Test
    void wrongPasswordsAndUnknownEmailsGetTheSameAnswer() {
        String wrongPassword = http.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", ADMIN_EMAIL, "password", "not the password"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody(JsonNode.class).returnResult().getResponseBody().get("detail").asText();
        String unknownEmail = http.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "nobody@franquix.local", "password", "not the password"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(JsonNode.class).returnResult().getResponseBody().get("detail").asText();

        assertThat(wrongPassword).isEqualTo(unknownEmail).isEqualTo("Invalid email or password");
    }

    @Test
    void weakPasswordsAreRejectedOnUserCreation() {
        client.post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "weak-" + System.nanoTime() + "@franquix.local", "name", "Weak",
                        "password", "123456789012", "role", "VIEWER"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("password must not be entirely numeric");
    }

    @Test
    void anonymousCallsToTheApiAreRejected() {
        http.get().uri("/api/v1/franchises")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
