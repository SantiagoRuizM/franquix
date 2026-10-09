package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.BranchTopProduct;
import com.franquix.franchises.domain.model.Franchise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Franchise operations. */
public interface FranchiseUseCase {

    /** Registers a new franchise with a globally unique name. */
    Mono<Franchise> create(String name);

    /** Changes the name of an existing franchise. */
    Mono<Franchise> rename(String franchiseId, String newName);

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
    Flux<BranchTopProduct> getTopStockProducts(String franchiseId);
}
