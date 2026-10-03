package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.port.in.DeleteProductUseCase;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public class DeleteProductService implements DeleteProductUseCase {

    private final HierarchyResolver hierarchy;
    private final ProductRepositoryPort products;

    public DeleteProductService(HierarchyResolver hierarchy, ProductRepositoryPort products) {
        this.hierarchy = hierarchy;
        this.products = products;
    }

    @Override
    public Mono<Void> delete(String franchiseId, String branchId, String productId) {
        return hierarchy.requireBranch(franchiseId, branchId)
                .flatMap(branch -> products.deleteByIdAndBranchId(productId, branch.id()))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(() -> NotFoundException.product(branchId, productId)))
                .then();
    }
}
