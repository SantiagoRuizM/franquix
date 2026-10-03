package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Branch;
import com.franquix.franchises.domain.port.in.AddBranchUseCase;
import com.franquix.franchises.domain.port.out.BranchRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class AddBranchService implements AddBranchUseCase {

    private final HierarchyResolver hierarchy;
    private final BranchRepositoryPort branches;
    private final Clock clock;

    public AddBranchService(HierarchyResolver hierarchy, BranchRepositoryPort branches, Clock clock) {
        this.hierarchy = hierarchy;
        this.branches = branches;
        this.clock = clock;
    }

    @Override
    public Mono<Branch> add(String franchiseId, String name) {
        return hierarchy.requireFranchise(franchiseId)
                .map(franchise -> Branch.create(franchise.id(), name, clock.instant()))
                .flatMap(branches::save);
    }
}
