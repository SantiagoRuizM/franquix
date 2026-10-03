package com.franquix.franchises.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full application over HTTP against MongoDB in Docker. Every E2E class shares this configuration, so
 * they reuse one Spring context and one container.
 *
 * <p>The bootstrap admin comes from the {@code local} profile defaults. Rate limits are raised because
 * the whole suite runs from one IP; {@code RateLimitTest} covers the limits themselves.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "franquix.security.rate-limit.auth-per-minute=10000",
                "franquix.security.rate-limit.anonymous-per-hour=10000",
                "franquix.security.rate-limit.user-per-hour=10000"})
// generous timeout: the first requests of a cold JVM also open the MongoDB connection pool
@AutoConfigureWebTestClient(timeout = "PT30S")
@Import(MongoTestcontainersConfiguration.class)
public abstract class E2ESupport {

    protected static final String ADMIN_EMAIL = "admin@franquix.local";
    protected static final String ADMIN_PASSWORD = "change-me-local-only";

    /** Unauthenticated client. */
    @Autowired
    protected WebTestClient http;

    /** Authenticated as the bootstrap ADMIN. */
    protected WebTestClient client;

    @BeforeEach
    void authenticateAsAdmin() {
        client = withToken(login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").asText());
    }

    protected WebTestClient withToken(String accessToken) {
        return http.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).build();
    }

    protected JsonNode login(String email, String password) {
        JsonNode body = http.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    protected static String unique(String prefix) {
        return prefix + " " + System.nanoTime();
    }
}
