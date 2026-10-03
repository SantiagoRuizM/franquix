package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Franchise;
import reactor.core.publisher.Mono;

/** Registers a new franchise with a globally unique name. */
@FunctionalInterface
public interface CreateFranchiseUseCase {

    Mono<Franchise> create(String name);
}
