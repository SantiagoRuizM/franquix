package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.FranchiseOverview;
import com.franquix.franchises.domain.model.auth.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

/** Access matrix: who may call what. */
class SecurityAccessTest extends RestTestSupport {

    @ParameterizedTest(name = "anonymous {0} {1} -> 401")
    @CsvSource({
            "GET, /api/v1/franchises",
            "GET, /api/v1/franchises/6650f1c2a1b2c3d4e5f60718",
            "POST, /api/v1/franchises",
            "DELETE, /api/v1/franchises/6650f1c2a1b2c3d4e5f60718/branches/6650f1c2a1b2c3d4e5f60719/products/6650f1c2a1b2c3d4e5f6071a",
            "POST, /api/v1/users",
            "GET, /api/v1/auth/me",
            "POST, /api/v1/auth/logout",
            "PATCH, /api/v1/auth/password",
            "GET, /api/v1/unknown"})
    void anonymousCallsAreRejectedWithAProblemDetail(HttpMethod method, String path) {
        anonymous.method(method).uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.detail").isEqualTo("Authentication required: missing, invalid or expired access token")
                .jsonPath("$.instance").isEqualTo(path);
        verifyNoInteractions(createFranchise, listFranchises, getFranchise, deleteProduct, registerUser);
    }

    @Test
    void aMalformedBearerTokenIsRejected() {
        anonymous.get().uri(BASE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void viewersCanRead() {
        when(getFranchise.get(FRANCHISE_ID)).thenReturn(Mono.just(
                new FranchiseOverview(new Franchise(FRANCHISE_ID, "Acme", NOW, NOW), List.of())));

        as(Role.VIEWER).get().uri(BASE + "/{id}", FRANCHISE_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest(name = "viewer {0} {1} -> 403")
    @CsvSource({
            "POST, /api/v1/franchises",
            "PATCH, /api/v1/franchises/6650f1c2a1b2c3d4e5f60718/name",
            "POST, /api/v1/franchises/6650f1c2a1b2c3d4e5f60718/branches",
            "PATCH, /api/v1/franchises/6650f1c2a1b2c3d4e5f60718/branches/6650f1c2a1b2c3d4e5f60719/products/6650f1c2a1b2c3d4e5f6071a/stock",
            "DELETE, /api/v1/franchises/6650f1c2a1b2c3d4e5f60718/branches/6650f1c2a1b2c3d4e5f60719/products/6650f1c2a1b2c3d4e5f6071a",
            "POST, /api/v1/users"})
    void viewersCannotWriteOrManageUsers(HttpMethod method, String path) {
        as(Role.VIEWER).method(method).uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "x", "stock", 1))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.detail").isEqualTo("You do not have permission to perform this operation");
        verifyNoInteractions(createFranchise, renameFranchise, addBranch, updateStock, deleteProduct, registerUser);
    }

    @Test
    void adminsCanWrite() {
        when(createFranchise.create(any()))
                .thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Acme", NOW, NOW)));

        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Acme"))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void authenticatedUnknownRoutesAreNotFound() {
        client.get().uri("/api/v1/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void responsesCarrySecurityHeaders() {
        client.get().uri("/api/v1/unknown")
                .exchange()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY");
    }
}
