package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

/**
 * Resolves the {@code franchise > branch > product} path of a request, failing with a
 * {@link NotFoundException} as soon as one level is missing or belongs to a different parent.
 */
public class HierarchyResolver {

    private final FranchiseRepositoryPort franchises;
    private final BranchRepositoryPort branches;
    private final ProductRepositoryPort products;

    public HierarchyResolver(
            FranchiseRepositoryPort franchises, BranchRepositoryPort branches, ProductRepositoryPort products) {
        this.franchises = franchises;
        this.branches = branches;
        this.products = products;
    }

    public Mono<Franchise> requireFranchise(String franchiseId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }

    public Mono<Branch> requireBranch(String franchiseId, String branchId) {
        return requireFranchise(franchiseId)
                .flatMap(franchise -> branches.findByIdAndFranchiseId(branchId, franchise.id()))
                .switchIfEmpty(Mono.error(() -> NotFoundException.branch(franchiseId, branchId)));
    }

    public Mono<Product> requireProduct(String franchiseId, String branchId, String productId) {
        return requireBranch(franchiseId, branchId)
                .flatMap(branch -> products.findByIdAndBranchId(productId, branch.id()))
                .switchIfEmpty(Mono.error(() -> NotFoundException.product(branchId, productId)));
    }
}
