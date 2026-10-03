package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.exception.NotFoundException;
import com.franquix.franchises.domain.model.DomainRules;
import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.port.in.RenameFranchiseUseCase;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class RenameFranchiseService implements RenameFranchiseUseCase {

    private final FranchiseRepositoryPort franchises;
    private final Clock clock;

    public RenameFranchiseService(FranchiseRepositoryPort franchises, Clock clock) {
        this.franchises = franchises;
        this.clock = clock;
    }

    @Override
    public Mono<Franchise> rename(String franchiseId, String newName) {
        return Mono.fromCallable(() -> DomainRules.requireValidName("name", newName))
                .flatMap(name -> franchises.rename(franchiseId, name, clock.instant()))
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }
}
