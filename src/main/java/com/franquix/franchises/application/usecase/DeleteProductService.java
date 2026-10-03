package com.franquix.franchises.application.usecase;

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
        return hierarchy.requireProduct(franchiseId, branchId, productId)
                .flatMap(product -> products.deleteById(product.id()));
    }
}
