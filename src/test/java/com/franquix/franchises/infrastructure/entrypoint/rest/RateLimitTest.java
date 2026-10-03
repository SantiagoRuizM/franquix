package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.AuthenticationFailedException;
import com.franquix.franchises.domain.model.PageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

/** Budgets are tiny here so the limits are reached quickly; production values are in application.yml. */
@TestPropertySource(properties = {
        "franquix.security.rate-limit.auth-per-minute=2",
        "franquix.security.rate-limit.user-per-hour=3",
        "franquix.security.rate-limit.anonymous-per-hour=1000"})
class RateLimitTest extends RestTestSupport {

    @Test
    void loginAttemptsAreLimitedPerClient() {
        when(login.login(any(), any())).thenReturn(Mono.error(AuthenticationFailedException.invalidCredentials()));

        for (int attempt = 0; attempt < 2; attempt++) {
            attemptLogin().expectStatus().isUnauthorized();
        }
        attemptLogin()
                .expectStatus().isEqualTo(429)
                .expectHeader().exists(HttpHeaders.RETRY_AFTER)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(429)
                .jsonPath("$.title").isEqualTo("Too Many Requests");
    }

    @Test
    void authenticatedRequestsAreLimitedPerUser() {
        when(listFranchises.list(any())).thenReturn(Mono.just(new PageResult<>(List.of(), 0, 20, 0)));

        for (int request = 0; request < 3; request++) {
            client.get().uri(BASE).exchange().expectStatus().isOk();
        }
        client.get().uri(BASE)
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectHeader().exists(HttpHeaders.RETRY_AFTER);
    }

    private org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec attemptLogin() {
        return anonymous.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "admin@franquix.local", "password", "guess-guess-guess"))
                .exchange();
    }
}
