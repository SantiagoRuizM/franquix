package com.franquix.franchises.application.usecase;

import com.franquix.franchises.domain.model.Franchise;
import com.franquix.franchises.domain.port.in.CreateFranchiseUseCase;
import com.franquix.franchises.domain.port.out.FranchiseRepositoryPort;
import java.time.Clock;
import reactor.core.publisher.Mono;

public class CreateFranchiseService implements CreateFranchiseUseCase {

    private final FranchiseRepositoryPort franchises;
    private final Clock clock;

    public CreateFranchiseService(FranchiseRepositoryPort franchises, Clock clock) {
        this.franchises = franchises;
        this.clock = clock;
    }

    @Override
    public Mono<Franchise> create(String name) {
        return Mono.fromCallable(() -> Franchise.create(name, clock.instant()))
                .flatMap(franchises::create);
    }
}
