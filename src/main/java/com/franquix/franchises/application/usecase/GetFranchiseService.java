package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.BranchOverview;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.model.FranchiseOverview;
import com.franquix.franchises.domain.port.in.GetFranchiseUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.ProductRepositoryPort;
import reactor.core.publisher.Mono;

public class GetFranchiseService implements GetFranchiseUseCase {

    private final HierarchyResolver hierarchy;
    private final BranchRepositoryPort branches;
    private final ProductRepositoryPort products;

    public GetFranchiseService(
            HierarchyResolver hierarchy, BranchRepositoryPort branches, ProductRepositoryPort products) {
        this.hierarchy = hierarchy;
        this.branches = branches;
        this.products = products;
    }

    @Override
    public Mono<FranchiseOverview> get(String franchiseId) {
        return hierarchy.requireFranchise(franchiseId).flatMap(this::loadTree);
    }

    private Mono<FranchiseOverview> loadTree(Franchise franchise) {
        return branches.findByFranchiseId(franchise.id())
                // flatMapSequential queries branches concurrently but keeps their name order
                .flatMapSequential(this::loadBranch)
                .collectList()
                .map(branchOverviews -> new FranchiseOverview(franchise, branchOverviews));
    }

    private Mono<BranchOverview> loadBranch(Branch branch) {
        return products.findByBranchId(branch.id())
                .collectList()
                .map(branchProducts -> new BranchOverview(branch, branchProducts));
    }
}
