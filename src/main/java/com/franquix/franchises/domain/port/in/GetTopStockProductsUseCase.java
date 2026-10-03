package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.BranchTopProduct;
import reactor.core.publisher.Flux;

/**
 * For a franchise, returns the product with the most stock in each branch.
 *
 * <ul>
 *   <li>Errors with {@code NotFoundException} when the franchise does not exist.</li>
 *   <li>Branches without products are omitted.</li>
 *   <li>Ties are broken by product name ascending.</li>
 *   <li>Results are ordered by branch name ascending.</li>
 * </ul>
 */
@FunctionalInterface
public interface GetTopStockProductsUseCase {

    Flux<BranchTopProduct> getTopStockProducts(String franchiseId);
}
