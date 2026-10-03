package com.franquix.franchises;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.franquix.franchises.support.MongoTestcontainersConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full application (HTTP server, use cases, MongoDB adapter and indexes) against MongoDB in Docker.
 * Each test creates its own franchise with a unique name, so tests stay independent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// generous timeout: the first requests of a cold JVM also open the MongoDB connection pool
@AutoConfigureWebTestClient(timeout = "PT30S")
@Import(MongoTestcontainersConfiguration.class)
class FranquixApiE2EIT {

    private static final String BASE = "/api/v1/franchises";

    @Autowired
    private WebTestClient client;

    private String createFranchise(String name) {
        return idOf(client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody());
    }

    private String addBranch(String franchiseId, String name) {
        return idOf(client.post().uri(BASE + "/{f}/branches", franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody());
    }

    private String addProduct(String franchiseId, String branchId, String name, int stock) {
        return idOf(client.post().uri(BASE + "/{f}/branches/{b}/products", franchiseId, branchId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name, "stock", stock))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody());
    }

    private static String idOf(JsonNode body) {
        assertThat(body).isNotNull();
        return body.get("id").asText();
    }

    private static String unique(String prefix) {
        return prefix + " " + System.nanoTime();
    }

    @Test
    void topStockProductsPerBranchHandlesTiesAndEmptyBranches() {
        String franchise = createFranchise(unique("Acme"));
        String norte = addBranch(franchise, "Norte");
        String centro = addBranch(franchise, "Centro");
        addBranch(franchise, "Empty");
        addProduct(franchise, centro, "Tea", 40);
        String coffee = addProduct(franchise, centro, "Coffee", 120);
        addProduct(franchise, norte, "Water", 85);
        String bagel = addProduct(franchise, norte, "Bagel", 85);

        client.get().uri(BASE + "/{f}/branches/top-stock-products", franchise)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].branchId").isEqualTo(centro)
                .jsonPath("$[0].branchName").isEqualTo("Centro")
                .jsonPath("$[0].productId").isEqualTo(coffee)
                .jsonPath("$[0].productName").isEqualTo("Coffee")
                .jsonPath("$[0].stock").isEqualTo(120)
                .jsonPath("$[1].branchName").isEqualTo("Norte")
                .jsonPath("$[1].productId").isEqualTo(bagel)
                .jsonPath("$[1].productName").isEqualTo("Bagel");
    }

    @Test
    void stockUpdatesAndDeletionsChangeTheTopProduct() {
        String franchise = createFranchise(unique("Stock"));
        String branch = addBranch(franchise, "Main");
        String coffee = addProduct(franchise, branch, "Coffee", 10);
        String tea = addProduct(franchise, branch, "Tea", 5);
        String product = BASE + "/{f}/branches/{b}/products/{p}";

        client.patch().uri(product + "/stock", franchise, branch, tea)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("stock", 50))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.stock").isEqualTo(50);
        client.get().uri(BASE + "/{f}/branches/top-stock-products", franchise)
                .exchange()
                .expectBody().jsonPath("$[0].productName").isEqualTo("Tea");

        client.delete().uri(product, franchise, branch, tea)
                .exchange()
                .expectStatus().isNoContent();
        client.get().uri(BASE + "/{f}/branches/top-stock-products", franchise)
                .exchange()
                .expectBody().jsonPath("$[0].productId").isEqualTo(coffee);

        client.delete().uri(product, franchise, branch, tea)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void renamesAllThreeLevelsAndReflectsThemInTheTree() {
        String franchise = createFranchise(unique("Old"));
        String branch = addBranch(franchise, "Old branch");
        String product = addProduct(franchise, branch, "Old product", 1);
        String newFranchiseName = unique("New");

        client.patch().uri(BASE + "/{f}/name", franchise)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "  " + newFranchiseName + "  "))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo(newFranchiseName);
        client.patch().uri(BASE + "/{f}/branches/{b}/name", franchise, branch)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "New branch"))
                .exchange()
                .expectStatus().isOk();
        client.patch().uri(BASE + "/{f}/branches/{b}/products/{p}/name", franchise, branch, product)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "New product"))
                .exchange()
                .expectStatus().isOk();

        client.get().uri(BASE + "/{f}", franchise)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo(newFranchiseName)
                .jsonPath("$.branches[0].name").isEqualTo("New branch")
                .jsonPath("$.branches[0].products[0].name").isEqualTo("New product");
    }

    @Test
    void duplicateNamesAreRejectedWith409AtEveryLevel() {
        String name = unique("Dup");
        String franchise = createFranchise(name);
        String branch = addBranch(franchise, "Downtown");
        addProduct(franchise, branch, "Coffee", 1);
        String otherBranch = addBranch(franchise, "Uptown");

        client.post().uri(BASE).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
        client.post().uri(BASE + "/{f}/branches", franchise).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Downtown"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
        client.patch().uri(BASE + "/{f}/branches/{b}/name", franchise, otherBranch)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Downtown"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
        client.post().uri(BASE + "/{f}/branches/{b}/products", franchise, branch)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Coffee", "stock", 3))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        // the same product name is fine in another branch
        addProduct(franchise, otherBranch, "Coffee", 3);
    }

    @Test
    void hierarchyIsEnforcedAcrossFranchises() {
        String acme = createFranchise(unique("Acme"));
        String other = createFranchise(unique("Other"));
        String branch = addBranch(acme, "Downtown");
        String product = addProduct(acme, branch, "Coffee", 1);

        client.post().uri(BASE + "/{f}/branches/{b}/products", other, branch)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Tea", "stock", 1))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail")
                .isEqualTo("Branch '" + branch + "' not found in franchise '" + other + "'");
        client.delete().uri(BASE + "/{f}/branches/{b}/products/{p}", other, branch, product)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void unknownFranchiseIs404AndMalformedIdIs400() {
        client.get().uri(BASE + "/{f}/branches/top-stock-products", "6650f1c2a1b2c3d4e5f60718")
                .exchange()
                .expectStatus().isNotFound();
        client.get().uri(BASE + "/{f}/branches/top-stock-products", "abc")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void listPagesThroughFranchisesInNameOrder() {
        createFranchise(unique("List A"));
        createFranchise(unique("List B"));

        client.get().uri(BASE + "?size=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.size").isEqualTo(1)
                .jsonPath("$.totalElements").value(total -> assertThat(((Number) total).longValue()).isGreaterThanOrEqualTo(2))
                .jsonPath("$.totalPages").value(pages -> assertThat(((Number) pages).longValue()).isGreaterThanOrEqualTo(2));
        client.get().uri(BASE + "?size=500")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void healthAndOpenApiAreExposed() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("Franquix API")
                .jsonPath("$.paths['/api/v1/franchises/{franchiseId}/branches/top-stock-products'].get.operationId")
                .isEqualTo("getTopStockProducts")
                .jsonPath("$.components.responses.NotFound").exists();
    }
}
