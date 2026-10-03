package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Franchise;
import reactor.core.publisher.Mono;

/** Changes the name of an existing franchise. */
@FunctionalInterface
public interface RenameFranchiseUseCase {

    Mono<Franchise> rename(String franchiseId, String newName);
}
