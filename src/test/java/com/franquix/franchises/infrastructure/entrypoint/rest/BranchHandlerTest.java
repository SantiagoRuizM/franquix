package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.DomainValidationException;
import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Branch;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

class BranchHandlerTest extends RestTestSupport {

    private static final String BRANCHES = BASE + "/{franchiseId}/branches";

    @Test
    void addReturns201WithLocationAndBody() {
        when(addBranch.add(FRANCHISE_ID, "Downtown"))
                .thenReturn(Mono.just(new Branch(BRANCH_ID, FRANCHISE_ID, "Downtown", NOW, NOW)));

        client.post().uri(BRANCHES, FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Downtown"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/branches/" + BRANCH_ID)
                .expectBody()
                .jsonPath("$.id").isEqualTo(BRANCH_ID)
                .jsonPath("$.franchiseId").isEqualTo(FRANCHISE_ID)
                .jsonPath("$.name").isEqualTo("Downtown");
    }

    @Test
    void addMapsMissingFranchisesTo404() {
        when(addBranch.add(FRANCHISE_ID, "Downtown")).thenReturn(Mono.error(NotFoundException.franchise(FRANCHISE_ID)));

        client.post().uri(BRANCHES, FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Downtown"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void addMapsDomainValidationErrorsTo400() {
        String tooLong = "x".repeat(101);
        when(addBranch.add(FRANCHISE_ID, tooLong))
                .thenReturn(Mono.error(new DomainValidationException("name must be at most 100 characters")));

        client.post().uri(BRANCHES, FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", tooLong))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("name must be at most 100 characters");
    }

    @Test
    void addRejectsAMalformedFranchiseIdBeforeReadingTheBody() {
        client.post().uri(BRANCHES, "123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Downtown"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.franchiseId").exists();
        verifyNoInteractions(addBranch);
    }

    @Test
    void renameReturnsTheRenamedBranch() {
        when(renameBranch.rename(FRANCHISE_ID, BRANCH_ID, "Uptown"))
                .thenReturn(Mono.just(new Branch(BRANCH_ID, FRANCHISE_ID, "Uptown", NOW, NOW)));

        client.patch().uri(BRANCHES + "/{branchId}/name", FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Uptown"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Uptown");
    }

    @Test
    void renameMapsDuplicatesTo409() {
        when(renameBranch.rename(FRANCHISE_ID, BRANCH_ID, "Uptown"))
                .thenReturn(Mono.error(DuplicateNameException.branch("Uptown")));

        client.patch().uri(BRANCHES + "/{branchId}/name", FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Uptown"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void renameRejectsAMalformedBranchId() {
        client.patch().uri(BRANCHES + "/{branchId}/name", FRANCHISE_ID, "zzz")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Uptown"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("Invalid branchId 'zzz'");
        verifyNoInteractions(renameBranch);
    }
}
