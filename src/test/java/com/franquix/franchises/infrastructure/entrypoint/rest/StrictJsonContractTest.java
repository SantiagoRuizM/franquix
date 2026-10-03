package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

/**
 * Regression tests for the audit findings on type coercion: values must arrive with the JSON type
 * the contract documents, otherwise 400 and nothing reaches the use case.
 */
class StrictJsonContractTest extends RestTestSupport {

    private static final String PRODUCTS = BASE + "/{franchiseId}/branches/{branchId}/products";

    @ParameterizedTest(name = "stock {0} is rejected")
    @ValueSource(strings = {"1.9", "1.0", "\"5\"", "true", "2147483648", "[1]", "{}"})
    void stockMustBeAJsonInteger(String stock) {
        client.post().uri(PRODUCTS, FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Coffee\",\"stock\":" + stock + "}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        verifyNoInteractions(addProduct);
    }

    @ParameterizedTest(name = "name {0} is rejected")
    @ValueSource(strings = {"123", "1.5", "true", "[\"a\"]", "{\"x\":1}"})
    void nameMustBeAJsonString(String name) {
        client.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":" + name + "}")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(createFranchise);
    }

    @ParameterizedTest(name = "stock update {0} is rejected")
    @ValueSource(strings = {"9.5", "\"9\"", "false"})
    void stockUpdateMustBeAJsonInteger(String stock) {
        client.patch().uri(PRODUCTS + "/{productId}/stock", FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"stock\":" + stock + "}")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(updateStock);
    }

    @ParameterizedTest(name = "unknown field {0} is rejected")
    @ValueSource(strings = {"\"stok\":5", "\"id\":\"x\""})
    void unknownFieldsAreRejected(String extra) {
        client.post().uri(PRODUCTS, FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Coffee\",\"stock\":1," + extra + "}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail")
                .isEqualTo("Malformed request: request body is not valid JSON or a field has the wrong type");
        verifyNoInteractions(addProduct);
    }
}
