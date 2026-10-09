package com.franquix.franchises.infrastructure.entrypoint.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.franquix.franchises.domain.exception.DuplicateNameException;
import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Product;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

class ProductHandlerTest extends RestTestSupport {

    private static final String PRODUCTS = BASE + "/{franchiseId}/branches/{branchId}/products";
    private static final String PRODUCT = PRODUCTS + "/{productId}";

    private static Product product(String name, int stock) {
        return new Product(PRODUCT_ID, BRANCH_ID, name, stock, NOW, NOW);
    }

    @Test
    void addReturns201WithLocationAndBody() {
        when(products.add(FRANCHISE_ID, BRANCH_ID, "Coffee", 120)).thenReturn(Mono.just(product("Coffee", 120)));

        client.post().uri(PRODUCTS, FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Coffee", "stock", 120))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/products/" + PRODUCT_ID)
                .expectBody()
                .jsonPath("$.id").isEqualTo(PRODUCT_ID)
                .jsonPath("$.branchId").isEqualTo(BRANCH_ID)
                .jsonPath("$.stock").isEqualTo(120);
    }

    @Test
    void addReportsEveryInvalidField() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "");
        body.put("stock", -1);

        client.post().uri(PRODUCTS, FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.name").isEqualTo("must not be blank")
                .jsonPath("$.errors.stock").isEqualTo("must be greater than or equal to 0");
        verifyNoInteractions(products);
    }

    @Test
    void addRequiresStock() {
        client.post().uri(PRODUCTS, FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Coffee"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.stock").isEqualTo("is required");
    }

    @Test
    void addRejectsANonNumericStock() {
        client.post().uri(PRODUCTS, FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Coffee", "stock", "lots"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail")
                .isEqualTo("Malformed request: request body is not valid JSON or a field has the wrong type");
    }

    @Test
    void addMapsDuplicatesTo409() {
        when(products.add(FRANCHISE_ID, BRANCH_ID, "Coffee", 1))
                .thenReturn(Mono.error(DuplicateNameException.product("Coffee")));

        client.post().uri(PRODUCTS, FRANCHISE_ID, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Coffee", "stock", 1))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void deleteReturns204() {
        when(products.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.empty());

        client.delete().uri(PRODUCT, FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void deleteMapsMissingProductsTo404() {
        when(products.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .thenReturn(Mono.error(NotFoundException.product(BRANCH_ID, PRODUCT_ID)));

        client.delete().uri(PRODUCT, FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail")
                .isEqualTo("Product '" + PRODUCT_ID + "' not found in branch '" + BRANCH_ID + "'");
    }

    @Test
    void deleteRejectsAMalformedProductId() {
        client.delete().uri(PRODUCT, FRANCHISE_ID, BRANCH_ID, "nope")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(products);
    }

    @Test
    void updateStockReturnsTheUpdatedProduct() {
        when(products.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 0)).thenReturn(Mono.just(product("Coffee", 0)));

        client.patch().uri(PRODUCT + "/stock", FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("stock", 0))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.stock").isEqualTo(0);
    }

    @Test
    void updateStockRejectsNegativeValues() {
        client.patch().uri(PRODUCT + "/stock", FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("stock", -10))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.stock").exists();
        verifyNoInteractions(products);
    }

    @Test
    void updateStockRejectsAMalformedBranchId() {
        client.patch().uri(PRODUCT + "/stock", FRANCHISE_ID, "x", PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("stock", 1))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("Invalid branchId 'x'");
    }

    @Test
    void renameReturnsTheRenamedProduct() {
        when(products.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
                .thenReturn(Mono.just(product("Espresso", 3)));

        client.patch().uri(PRODUCT + "/name", FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Espresso"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Espresso");
    }

    @Test
    void renameMapsMissingProductsTo404() {
        when(products.rename(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
                .thenReturn(Mono.error(NotFoundException.product(BRANCH_ID, PRODUCT_ID)));

        client.patch().uri(PRODUCT + "/name", FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Espresso"))
                .exchange()
                .expectStatus().isNotFound();
    }
}
