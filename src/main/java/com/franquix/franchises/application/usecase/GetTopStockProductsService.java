package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.BranchTopProduct;
import com.franquix.franchises.domain.port.in.GetTopStockProductsUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Streams the branches of a franchise and, for each one, asks the repository for its single
 * top-stock product (an indexed {@code sort + limit 1} query). Branches whose lookup is empty are
 * dropped, which is how branches without products are omitted.
 */
public class GetTopStockProductsService implements GetTopStockProductsUseCase {

    /** Caps the per-branch queries a single request runs at once, protecting the connection pool. */
    private static final int BRANCH_LOOKUP_CONCURRENCY = 8;

    private final HierarchyResolver hierarchy;
    private final BranchRepositoryPort branches;
    private final ProductRepositoryPort products;

    public GetTopStockProductsService(
            HierarchyResolver hierarchy, BranchRepositoryPort branches, ProductRepositoryPort products) {
        this.hierarchy = hierarchy;
        this.branches = branches;
        this.products = products;
    }

    @Override
    public Flux<BranchTopProduct> getTopStockProducts(String franchiseId) {
        return hierarchy.requireFranchise(franchiseId)
                .flatMapMany(franchise -> branches.findByFranchiseId(franchise.id()))
                // concurrent lookups, results emitted in the branch (name) order
                .flatMapSequential(this::topProductOf, BRANCH_LOOKUP_CONCURRENCY);
    }

    private Mono<BranchTopProduct> topProductOf(Branch branch) {
        return products.findTopStockByBranchId(branch.id())
                .map(product -> BranchTopProduct.of(branch, product));
    }
}
