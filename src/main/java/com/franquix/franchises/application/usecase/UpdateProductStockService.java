package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.DomainRules;
import com.franquix.franchises.domain.model.Product;
import com.franquix.franchises.domain.port.in.UpdateProductStockUseCase;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class UpdateProductStockService implements UpdateProductStockUseCase {

    private final HierarchyResolver hierarchy;
    private final ProductRepositoryPort products;
    private final Clock clock;

    public UpdateProductStockService(HierarchyResolver hierarchy, ProductRepositoryPort products, Clock clock) {
        this.hierarchy = hierarchy;
        this.products = products;
        this.clock = clock;
    }

    @Override
    public Mono<Product> updateStock(String franchiseId, String branchId, String productId, int stock) {
        return Mono.fromCallable(() -> DomainRules.requireValidStock(stock))
                .flatMap(validStock -> hierarchy.requireBranch(franchiseId, branchId)
                        .flatMap(branch -> products.updateStock(productId, branch.id(), validStock, clock.instant()))
                        .switchIfEmpty(Mono.error(() -> NotFoundException.product(branchId, productId))));
    }
}
