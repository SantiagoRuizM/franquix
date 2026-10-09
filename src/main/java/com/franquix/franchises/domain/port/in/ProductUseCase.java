package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Product;
import reactor.core.publisher.Mono;

/**
 * Product operations. Every one checks the {@code franchise > branch > product} path and fails with
 * {@code NotFoundException} as soon as one level is missing or belongs to another parent.
 */
public interface ProductUseCase {

    /** Adds a new product to a branch. */
    Mono<Product> add(String franchiseId, String branchId, String name, int stock);

    /** Removes a product from its branch. */
    Mono<Void> delete(String franchiseId, String branchId, String productId);

    /** Sets the stock of a product. */
    Mono<Product> updateStock(String franchiseId, String branchId, String productId, int stock);

    /** Changes the name of a product. */
    Mono<Product> rename(String franchiseId, String branchId, String productId, String newName);
}
