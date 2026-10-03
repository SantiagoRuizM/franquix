package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.model.DomainRules;
import com.franquix.franchises.domain.port.in.RenameBranchUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class RenameBranchService implements RenameBranchUseCase {

    private final HierarchyResolver hierarchy;
    private final BranchRepositoryPort branches;
    private final Clock clock;

    public RenameBranchService(HierarchyResolver hierarchy, BranchRepositoryPort branches, Clock clock) {
        this.hierarchy = hierarchy;
        this.branches = branches;
        this.clock = clock;
    }

    @Override
    public Mono<Branch> rename(String franchiseId, String branchId, String newName) {
        return Mono.fromCallable(() -> DomainRules.requireValidName("name", newName))
                .flatMap(name -> hierarchy.requireFranchise(franchiseId)
                        .flatMap(franchise -> branches.rename(branchId, franchise.id(), name, clock.instant()))
                        .switchIfEmpty(Mono.error(() -> NotFoundException.branch(franchiseId, branchId))));
    }
}
