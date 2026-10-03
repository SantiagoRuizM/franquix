package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.in.AddProductUseCase;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class AddProductService implements AddProductUseCase {

    private final HierarchyResolver hierarchy;
    private final ProductRepositoryPort products;
    private final Clock clock;

    public AddProductService(HierarchyResolver hierarchy, ProductRepositoryPort products, Clock clock) {
        this.hierarchy = hierarchy;
        this.products = products;
        this.clock = clock;
    }

    @Override
    public Mono<Product> add(String franchiseId, String branchId, String name, int stock) {
        return hierarchy.requireBranch(franchiseId, branchId)
                .map(branch -> Product.create(branch.id(), name, stock, clock.instant()))
                .flatMap(products::save);
    }
}
