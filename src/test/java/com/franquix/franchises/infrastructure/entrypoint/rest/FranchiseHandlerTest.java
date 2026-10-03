package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.BranchOverview;
import com.franquix.franchises.domain.model.BranchTopProduct;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.FranchiseOverview;
import com.franquix.franchises.domain.model.Product;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class FranchiseHandlerTest extends RestTestSupport {

    private static final Franchise ACME = new Franchise(FRANCHISE_ID, "Acme", NOW, NOW);

    @Test
    void createReturns201WithLocationAndBody() {
        when(createFranchise.create("Acme")).thenReturn(Mono.just(ACME));

        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Acme"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/v1/franchises/" + FRANCHISE_ID)
                .expectBody()
                .jsonPath("$.id").isEqualTo(FRANCHISE_ID)
                .jsonPath("$.name").isEqualTo("Acme")
                .jsonPath("$.createdAt").isEqualTo("2026-03-15T12:30:00Z");
    }

    @Test
    void createRejectsABlankNameWithFieldErrors() {
        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "   "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request validation failed")
                .jsonPath("$.errors.name").isEqualTo("must not be blank")
                .jsonPath("$.instance").isEqualTo(BASE)
                .jsonPath("$.timestamp").isEqualTo("2026-03-15T12:30:00Z");
        verifyNoInteractions(createFranchise);
    }

    @Test
    void createRejectsAMissingBody() {
        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("Request body is required");
    }

    @Test
    void createRejectsMalformedJson() {
        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").value(detail ->
                        assertThat((String) detail).startsWith("Malformed request"));
    }

    @Test
    void createRejectsUnsupportedMediaType() {
        client.post().uri(BASE)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("Acme")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void createMapsDuplicateNamesTo409() {
        when(createFranchise.create("Acme")).thenReturn(Mono.error(DuplicateNameException.franchise("Acme")));

        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Acme"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conflict")
                .jsonPath("$.detail").isEqualTo("A franchise named 'Acme' already exists");
    }

    @Test
    void getReturnsTheWholeTree() {
        Branch branch = new Branch(BRANCH_ID, FRANCHISE_ID, "Downtown", NOW, NOW);
        Product product = new Product(PRODUCT_ID, BRANCH_ID, "Coffee", 7, NOW, NOW);
        when(getFranchise.get(FRANCHISE_ID)).thenReturn(Mono.just(
                new FranchiseOverview(ACME, List.of(new BranchOverview(branch, List.of(product))))));

        client.get().uri(BASE + "/{id}", FRANCHISE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Acme")
                .jsonPath("$.branches[0].name").isEqualTo("Downtown")
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Coffee")
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(7);
    }

    @Test
    void getRejectsAMalformedId() {
        client.get().uri(BASE + "/{id}", "not-an-id")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Invalid franchiseId 'not-an-id'")
                .jsonPath("$.errors.franchiseId").exists();
        verifyNoInteractions(getFranchise);
    }

    @Test
    void getMapsMissingFranchisesTo404() {
        when(getFranchise.get(FRANCHISE_ID)).thenReturn(Mono.error(NotFoundException.franchise(FRANCHISE_ID)));

        client.get().uri(BASE + "/{id}", FRANCHISE_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.detail").isEqualTo("Franchise '" + FRANCHISE_ID + "' not found")
                .jsonPath("$.instance").isEqualTo(BASE + "/" + FRANCHISE_ID);
    }

    @Test
    void renameReturnsTheRenamedFranchise() {
        when(renameFranchise.rename(FRANCHISE_ID, "Acme Global"))
                .thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Acme Global", NOW, NOW)));

        client.patch().uri(BASE + "/{id}/name", FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Acme Global"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Acme Global");
    }

    @Test
    void renameRejectsAMissingName() {
        client.patch().uri(BASE + "/{id}/name", FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.name").isEqualTo("must not be blank");
        verifyNoInteractions(renameFranchise);
    }

    @Test
    void topStockProductsReturnsOneEntryPerBranch() {
        when(getTopStockProducts.getTopStockProducts(FRANCHISE_ID)).thenReturn(Flux.just(
                new BranchTopProduct("b1", "Centro", "p9", "Coffee", 120),
                new BranchTopProduct("b2", "Norte", "p4", "Tea", 85)));

        client.get().uri(BASE + "/{id}/branches/top-stock-products", FRANCHISE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].branchId").isEqualTo("b1")
                .jsonPath("$[0].branchName").isEqualTo("Centro")
                .jsonPath("$[0].productId").isEqualTo("p9")
                .jsonPath("$[0].productName").isEqualTo("Coffee")
                .jsonPath("$[0].stock").isEqualTo(120)
                .jsonPath("$[1].branchName").isEqualTo("Norte");
    }

    @Test
    void topStockProductsReturnsAnEmptyArrayWhenNoBranchHasProducts() {
        when(getTopStockProducts.getTopStockProducts(FRANCHISE_ID)).thenReturn(Flux.empty());

        client.get().uri(BASE + "/{id}/branches/top-stock-products", FRANCHISE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void topStockProductsMapsMissingFranchisesTo404() {
        when(getTopStockProducts.getTopStockProducts(anyString()))
                .thenReturn(Flux.error(NotFoundException.franchise(FRANCHISE_ID)));

        client.get().uri(BASE + "/{id}/branches/top-stock-products", FRANCHISE_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
