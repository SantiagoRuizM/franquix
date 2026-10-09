package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.BranchTopProduct;
import com.franquix.franchises.domain.model.DomainRules;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.port.in.FranchiseUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FranchiseService implements FranchiseUseCase {

    private final FranchiseRepositoryPort franchises;
    private final BranchRepositoryPort branches;
    private final ProductRepositoryPort products;
    private final Clock clock;

    public FranchiseService(
            FranchiseRepositoryPort franchises,
            BranchRepositoryPort branches,
            ProductRepositoryPort products,
            Clock clock) {
        this.franchises = franchises;
        this.branches = branches;
        this.products = products;
        this.clock = clock;
    }

    @Override
    public Mono<Franchise> create(String name) {
        return Mono.fromCallable(() -> Franchise.create(name, clock.instant()))
                .flatMap(franchises::create);
    }

    @Override
    public Mono<Franchise> rename(String franchiseId, String newName) {
        return Mono.fromCallable(() -> DomainRules.requireValidName("name", newName))
                .flatMap(name -> franchises.rename(franchiseId, name, clock.instant()))
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }

    /**
     * Streams the branches of the franchise and, for each one, asks the repository for its single
     * top-stock product (an indexed {@code sort + limit 1} query). Branches whose lookup is empty are
     * dropped, which is how branches without products are omitted.
     */
    @Override
    public Flux<BranchTopProduct> getTopStockProducts(String franchiseId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)))
                .flatMapMany(franchise -> branches.findByFranchiseId(franchise.id()))
                // concurrent lookups, results emitted in the branch (name) order
                .flatMapSequential(this::topProductOf);
    }

    private Mono<BranchTopProduct> topProductOf(Branch branch) {
        return products.findTopStockByBranchId(branch.id())
                .map(product -> BranchTopProduct.of(branch, product));
    }
}
