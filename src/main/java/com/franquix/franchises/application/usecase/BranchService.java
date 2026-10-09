package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.DomainRules;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.port.in.BranchUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import reactor.core.publisher.Mono;

public class BranchService implements BranchUseCase {

    private final FranchiseRepositoryPort franchises;
    private final BranchRepositoryPort branches;

    public BranchService(FranchiseRepositoryPort franchises, BranchRepositoryPort branches) {
        this.franchises = franchises;
        this.branches = branches;
    }

    @Override
    public Mono<Branch> add(String franchiseId, String name) {
        return requireFranchise(franchiseId)
                .map(franchise -> Branch.create(franchise.id(), name))
                .flatMap(branches::create);
    }

    @Override
    public Mono<Branch> rename(String franchiseId, String branchId, String newName) {
        return Mono.fromCallable(() -> DomainRules.requireValidName("name", newName))
                .flatMap(name -> requireFranchise(franchiseId)
                        .flatMap(franchise -> branches.rename(branchId, franchise.id(), name))
                        .switchIfEmpty(Mono.error(() -> NotFoundException.branch(franchiseId, branchId))));
    }

    private Mono<Franchise> requireFranchise(String franchiseId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }
}
