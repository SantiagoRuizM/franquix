package com.franquix.franchises.domain.port.in;

import com.franquix.franchises.domain.model.Branch;
import reactor.core.publisher.Mono;

/** Adds a new branch to an existing franchise. */
@FunctionalInterface
public interface AddBranchUseCase {

    Mono<Branch> add(String franchiseId, String name);
}
