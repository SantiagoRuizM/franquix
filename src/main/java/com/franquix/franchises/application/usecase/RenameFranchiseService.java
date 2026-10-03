package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.port.in.RenameFranchiseUseCase;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class RenameFranchiseService implements RenameFranchiseUseCase {

    private final HierarchyResolver hierarchy;
    private final FranchiseRepositoryPort franchises;
    private final Clock clock;

    public RenameFranchiseService(HierarchyResolver hierarchy, FranchiseRepositoryPort franchises, Clock clock) {
        this.hierarchy = hierarchy;
        this.franchises = franchises;
        this.clock = clock;
    }

    @Override
    public Mono<Franchise> rename(String franchiseId, String newName) {
        return hierarchy.requireFranchise(franchiseId)
                .map(franchise -> franchise.rename(newName, clock.instant()))
                .flatMap(franchises::save);
    }
}
