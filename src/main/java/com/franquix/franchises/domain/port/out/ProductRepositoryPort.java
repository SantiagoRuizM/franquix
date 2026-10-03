package com.franquix.franchises.domain.port.out;

import com.franquix.franchises.domain.model.Product;
import java.time.Instant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Persistence port for products.
 *
 * <p>Updates are atomic and field-level: they never recreate a deleted product and never overwrite
 * a field changed concurrently by another request.
 */
public interface ProductRepositoryPort {

    /**
     * Inserts a new product.
     *
     * @return the stored product; errors with
     *     {@link com.franquix.franchises.domain.exception.DuplicateNameException} when the name is
     *     already used inside the same branch
     */
    Mono<Product> create(Product product);

    /** @return the updated product, or empty when it does not exist in that branch */
    Mono<Product> rename(String productId, String branchId, String name, Instant updatedAt);

    /** @return the updated product, or empty when it does not exist in that branch */
    Mono<Product> updateStock(String productId, String branchId, int stock, Instant updatedAt);

    /** @return the product only when it exists and belongs to the branch, otherwise empty */
    Mono<Product> findByIdAndBranchId(String productId, String branchId);

    /** @return every product of the branch, ordered by name ascending */
    Flux<Product> findByBranchId(String branchId);

    /**
     * @return the product with the highest stock of the branch (ties broken by name ascending), or
     *     empty when the branch has no products
     */
    Mono<Product> findTopStockByBranchId(String branchId);

    /**
     * Deletes the product only if it belongs to the branch.
     *
     * @return {@code true} when a product was deleted
     */
    Mono<Boolean> deleteByIdAndBranchId(String productId, String branchId);
}
