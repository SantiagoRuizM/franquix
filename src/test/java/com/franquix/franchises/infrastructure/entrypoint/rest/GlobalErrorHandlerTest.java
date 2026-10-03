package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

class GlobalErrorHandlerTest extends RestTestSupport {

    @Test
    void unknownRoutesAreProblemDetails() {
        client.get().uri("/api/v1/unknown")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.instance").isEqualTo("/api/v1/unknown");
    }

    @Test
    void unsupportedMethodsAreProblemDetails() {
        client.put().uri(BASE + "/{id}", FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "x"))
                .exchange()
                .expectStatus().value(status -> assertThat(status).isIn(404, 405))
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void unexpectedErrorsBecomeA500WithoutInternals() {
        when(getFranchise.get(FRANCHISE_ID))
                .thenReturn(Mono.error(new IllegalStateException("connection pool exhausted at db-01:27017")));

        client.get().uri(BASE + "/{id}", FRANCHISE_ID)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Internal Server Error")
                .jsonPath("$.detail").isEqualTo("An unexpected error occurred")
                .jsonPath("$.trace").doesNotExist()
                .jsonPath("$.exception").doesNotExist();
    }
}
