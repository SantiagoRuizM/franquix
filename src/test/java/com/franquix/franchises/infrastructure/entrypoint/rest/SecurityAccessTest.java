package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.model.Franchise;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

/** The bearer token guards every /api route. */
class SecurityAccessTest extends RestTestSupport {

    @Test
    void requestsWithoutATokenAreRejectedWithAProblemDetail() {
        anonymous.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Acme"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.detail").isEqualTo("Missing or invalid bearer token")
                .jsonPath("$.instance").isEqualTo(BASE);
        verifyNoInteractions(franchises);
    }

    @ParameterizedTest(name = "Authorization: {0}")
    @ValueSource(strings = {"Bearer wrong-token", "Bearer ", "local-dev-token", "Basic bG9jYWw6ZGV2"})
    void wrongOrMalformedTokensAreRejected(String authorization) {
        anonymous.get().uri(BASE + "/{id}/branches/top-stock-products", FRANCHISE_ID)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .exchange()
                .expectStatus().isUnauthorized();
        verifyNoInteractions(franchises);
    }

    @Test
    void theRightTokenIsAccepted() {
        when(franchises.create(any())).thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Acme")));

        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Acme"))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void routesOutsideTheApiArePublic() {
        anonymous.get().uri("/not-api")
                .exchange()
                .expectStatus().isNotFound();
    }
}
