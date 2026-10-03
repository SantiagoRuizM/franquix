package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.in.RenameProductUseCase;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class RenameProductService implements RenameProductUseCase {

    private final HierarchyResolver hierarchy;
    private final ProductRepositoryPort products;
    private final Clock clock;

    public RenameProductService(HierarchyResolver hierarchy, ProductRepositoryPort products, Clock clock) {
        this.hierarchy = hierarchy;
        this.products = products;
        this.clock = clock;
    }

    @Override
    public Mono<Product> rename(String franchiseId, String branchId, String productId, String newName) {
        return hierarchy.requireProduct(franchiseId, branchId, productId)
                .map(product -> product.rename(newName, clock.instant()))
                .flatMap(products::save);
    }
}
